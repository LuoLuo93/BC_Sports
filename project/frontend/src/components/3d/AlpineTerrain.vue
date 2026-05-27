<template>
  <div ref="containerRef" class="alpine-terrain">
    <button class="fullscreen-btn" @click="toggleFullscreen" title="全屏">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path v-if="!isFullscreen" d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
        <path v-else d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as THREE from 'three'

const containerRef = ref(null)
const isFullscreen = ref(false)

function toggleFullscreen() {
  const el = containerRef.value
  if (!el) return
  if (!document.fullscreenElement) {
    el.requestFullscreen?.().catch(() => {})
  } else {
    document.exitFullscreen?.().catch(() => {})
  }
}

function onFSChange() {
  isFullscreen.value = !!document.fullscreenElement
}

let scene, camera, renderer
let terrainGroup, trailDots = [], snowParticles
let animationId = null
let resizeObserver = null
let startTime = 0
let trailCurve = null

// Camera orbit state
let orbitDistance = 28
let orbitHeight = 1.8
const orbitTarget = new THREE.Vector3(0, 3.0, 0)

// ─── Noise helpers ──────────────────────────
function hash(x, z) {
  let n = Math.sin(x * 127.1 + z * 311.7) * 43758.5453
  return n - Math.floor(n)
}
function smoothNoise(x, z) {
  const ix = Math.floor(x), iz = Math.floor(z)
  const fx = x - ix, fz = z - iz
  const sx = fx * fx * (3 - 2 * fx), sz = fz * fz * (3 - 2 * fz)
  const a = hash(ix, iz), b = hash(ix + 1, iz)
  const c = hash(ix, iz + 1), d = hash(ix + 1, iz + 1)
  return a + (b - a) * sx + (c - a) * sz + (a - b - c + d) * sx * sz
}
function fbm(x, z, oct) {
  let v = 0, amp = 0.5, freq = 1
  for (let i = 0; i < oct; i++) {
    v += amp * smoothNoise(x * freq, z * freq)
    amp *= 0.5; freq *= 2.1
  }
  return v
}

// ─── Everest-inspired terrain ────────────────
// Real peaks (mapped to local coords):
//   Everest  8848m → ( 0.0,  0.0)  main
//   Lhotse   8516m → ( 0.6, -0.7)  south
//   Nuptse   7861m → (-0.4, -1.1)  southwest ridge
//   Changtse 7583m → (-0.9,  0.5)  north
//   Khumbutse 6665m→ (-1.8,  0.0)  west

function getHeight(x, z) {
  // Main Everest — sharp triangular peak with NE ridge asymmetry
  const dE = Math.sqrt(x * x + z * z)
  const everest = 4.8 * Math.exp(-dE * dE / 0.28)
    + 0.6 * Math.exp(-((x - 0.15) ** 2 + (z - 0.05) ** 2) / 0.08) // Hillary step bump

  // Lhotse — steep south face
  const dL = Math.sqrt((x - 0.6) ** 2 + (z + 0.7) ** 2)
  const lhotse = 4.2 * Math.exp(-dL * dL / 0.22)

  // Nuptse — long SW ridge
  const dN = Math.sqrt((x + 0.4) ** 2 + (z + 1.1) ** 2)
  const nuptse = 3.4 * Math.exp(-dN * dN / 0.35)
    + 0.8 * Math.exp(-((x + 0.1) ** 2 + (z + 0.5) ** 2) / 0.2) // ridge extension

  // Changtse (North)
  const dC = Math.sqrt((x + 0.9) ** 2 + (z - 0.5) ** 2)
  const changtse = 2.8 * Math.exp(-dC * dC / 0.4)

  // Khumbutse (West)
  const dK = Math.sqrt((x + 1.8) ** 2 + z * z)
  const khumbutse = 2.2 * Math.exp(-dK * dK / 0.5)

  // South Col saddle between Everest and Lhotse
  const col = -0.8 * Math.exp(-((x - 0.3) ** 2 + (z + 0.35) ** 2) / 0.06)

  // Western Cwm valley
  const cwm = -0.5 * Math.exp(-((x - 0.2) ** 2 + (z + 0.2) ** 2) / 0.08)

  // Khumbu Icefall area
  const icefall = -0.4 * Math.exp(-((x + 0.3) ** 2 + (z + 0.8) ** 2) / 0.1)

  // Rongbuk valley (north)
  const rongbuk = -0.3 * Math.exp(-((x - 0.2) ** 2 + (z - 0.8) ** 2) / 0.15)

  // Foothills & noise
  const micro = fbm(x * 1.5 + 3.7, z * 1.5 + 2.1, 5) * 0.45
  const ridge = 0.15 * Math.sin(x * 3.5 + 1.2) * Math.cos(z * 2.8 + 0.6)
    + 0.1 * Math.sin(x * 5.2 + z * 4.1)

  const base = 0.2 + 0.15 * Math.exp(-(x * x + z * z) / 8)

  return Math.max(0, everest + lhotse + nuptse + changtse + khumbutse
    + col + cwm + icefall + rongbuk + micro + ridge + base)
}

// ─── Color by altitude ───────────────────────
function heightToColor(y) {
  const t = y / 4.8
  // More dramatic: dark earth → warm rock → crisp snow → glacial ice
  const valley   = new THREE.Color(0x5C4A3A)  // darker earth
  const lowRock  = new THREE.Color(0x7a8a9e)  // cooler slate
  const highRock = new THREE.Color(0xc8d2dc)  // bright rock
  const snow     = new THREE.Color(0xf8faff)  // warm white
  const iceBlue  = new THREE.Color(0x7dd3fc)  // vibrant ice

  if (t < 0.12) return valley.clone().lerp(lowRock, t / 0.12)
  if (t < 0.28) return lowRock.clone().lerp(highRock, (t - 0.12) / 0.16)
  if (t < 0.50) return highRock.clone().lerp(snow, (t - 0.28) / 0.22)
  if (t < 0.78) return snow.clone().lerp(snow, (t - 0.50) / 0.28)
  return snow.clone().lerp(iceBlue, (t - 0.78) / 0.22)
}

// ─── Contour lines (marching iso-lines) ──────
function buildContourLines(terrainGeo, seg, size) {
  const lines = []
  const half = size / 2
  const step = size / seg
  const levels = [0.6, 1.2, 1.8, 2.4, 3.0, 3.6, 4.2, 4.6]

  levels.forEach(level => {
    const pts = []
    for (let i = 0; i < seg; i++) {
      for (let j = 0; j < seg; j++) {
        const x0 = -half + j * step, z0 = -half + i * step
        const x1 = x0 + step, z1 = z0 + step
        const h00 = getHeight(x0, z0), h10 = getHeight(x1, z0)
        const h01 = getHeight(x0, z1), h11 = getHeight(x1, z1)
        // March edges of this quad cell
        const edges = [
          [x0, h00, z0, x1, h10, z0],
          [x1, h10, z0, x1, h11, z1],
          [x1, h11, z1, x0, h01, z1],
          [x0, h01, z1, x0, h00, z0]
        ]
        for (const [ax, ay, az, bx, by, bz] of edges) {
          if ((ay - level) * (by - level) < 0) {
            const t = (level - ay) / (by - ay)
            pts.push(new THREE.Vector3(
              ax + t * (bx - ax),
              level + 0.015,
              az + t * (bz - az)
            ))
          }
        }
      }
    }
    if (pts.length > 2) lines.push(pts)
  })
  return lines
}

// ─── South Col Route trail ───────────────────
function buildTrailCurve() {
  const waypoints = [
    [-1.2, -1.5],   // Base Camp 5364m
    [-0.6, -1.0],   // Icefall
    [-0.1, -0.5],   // Camp 1
    [0.05, -0.2],   // Western Cwm
    [0.1, -0.1],    // Camp 2
    [0.15, 0.05],   // Lhotse Face
    [0.2, 0.15],    // Camp 3
    [0.25, 0.25],   // Geneva Spur
    [0.3, 0.35],    // South Col (Camp 4)
    [0.25, 0.5],    // Balcony
    [0.2, 0.7],     // South Summit
    [0.15, 0.9],    // Hillary Step
    [0.0, 0.0],     // SUMMIT
  ]
  const pts = waypoints.map(([x, z]) => {
    const y = getHeight(x, z) + 0.04
    return new THREE.Vector3(x, y, z)
  })
  return new THREE.CatmullRomCurve3(pts, false, 'catmullrom', 0.3)
}

// ─── Sprite label ────────────────────────────
function makeLabel(text, sub, color = '#0f172a', bg = 'rgba(255,255,255,0.88)') {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  const dpr = 2
  const useSub = !!sub
  canvas.width = (useSub ? 340 : 200) * dpr
  canvas.height = 42 * dpr
  ctx.scale(dpr, dpr)

  // Background pill
  ctx.fillStyle = bg
  const w = useSub ? 340 : 200, h = 42
  const r = 6
  ctx.beginPath()
  ctx.moveTo(r, 3); ctx.lineTo(w - r, 3); ctx.quadraticCurveTo(w - 3, 3, w - 3, r + 3)
  ctx.lineTo(w - 3, h - r - 3); ctx.quadraticCurveTo(w - 3, h - 3, w - r, h - 3)
  ctx.lineTo(r, h - 3); ctx.quadraticCurveTo(3, h - 3, 3, h - r - 3)
  ctx.lineTo(3, r + 3); ctx.quadraticCurveTo(3, 3, r, 3)
  ctx.closePath(); ctx.fill()

  // Border
  ctx.strokeStyle = 'rgba(14,165,233,0.25)'
  ctx.lineWidth = 1
  ctx.stroke()

  if (useSub) {
    // Divider
    ctx.strokeStyle = 'rgba(14,165,233,0.15)'
    ctx.beginPath()
    ctx.moveTo(w / 2, 8); ctx.lineTo(w / 2, h - 8)
    ctx.stroke()

    // Main text (left half)
    ctx.fillStyle = color
    ctx.font = 'bold 20px Outfit, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(text, w * 0.27, h / 2)

    // Sub text (right half)
    ctx.fillStyle = '#0f172a'
    ctx.font = 'bold 13px Outfit, sans-serif'
    ctx.fillText(sub, w * 0.73, h / 2)
  } else {
    // Main text centered
    ctx.fillStyle = color
    ctx.font = 'bold 20px Outfit, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(text, w / 2, h / 2)
  }

  const tex = new THREE.CanvasTexture(canvas)
  tex.minFilter = THREE.LinearFilter
  const spriteMat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthTest: false })
  const sprite = new THREE.Sprite(spriteMat)
  sprite.scale.set(useSub ? 1.5 : 0.9, 0.2, 1)
  return sprite
}

// ─── Flag sprite ─────────────────────────────
function makeFlag(color) {
  const canvas = document.createElement('canvas')
  const dpr = 2
  canvas.width = 64 * dpr; canvas.height = 80 * dpr
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)
  // Pole
  ctx.fillStyle = '#64748b'
  ctx.fillRect(28, 8, 2, 68)
  // Flag
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.moveTo(30, 8); ctx.lineTo(58, 18); ctx.lineTo(30, 28)
  ctx.closePath(); ctx.fill()
  const tex = new THREE.CanvasTexture(canvas)
  tex.minFilter = THREE.LinearFilter
  const mat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthTest: false })
  const sprite = new THREE.Sprite(mat)
  sprite.scale.set(0.25, 0.32, 1)
  return sprite
}

function init() {
  const container = containerRef.value
  if (!container) return
  const w = container.clientWidth
  const h = container.clientHeight
  if (w === 0 || h === 0) return

  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0xf1f5f9, 0.022)

  camera = new THREE.PerspectiveCamera(22, w / h, 0.1, 500)
  camera.position.set(orbitDistance * 0.7, orbitHeight, orbitDistance * 0.7)
  camera.lookAt(orbitTarget)

  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'high-performance' })
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2))
  renderer.setSize(w, h)
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.0
  renderer.outputColorSpace = THREE.SRGBColorSpace
  container.appendChild(renderer.domElement)

  // ── Lights ──
  scene.add(new THREE.HemisphereLight(0xc7e2fb, 0x7a6b55, 0.6))

  const sun = new THREE.DirectionalLight(0xfff5e6, 1.6)
  sun.position.set(8, 18, 10)
  scene.add(sun)

  const fill = new THREE.DirectionalLight(0x93c5fd, 0.5)
  fill.position.set(-8, 6, -6)
  scene.add(fill)

  const rim = new THREE.DirectionalLight(0xfde68a, 0.6)
  rim.position.set(-5, 4, 12)
  scene.add(rim)

  // Soft top-down accent
  const top = new THREE.DirectionalLight(0xe0f2fe, 0.25)
  top.position.set(0, 20, 0)
  scene.add(top)

  // ── Atmosphere glow ──
  const glowGeo = new THREE.SphereGeometry(6.5, 32, 32)
  const glowMat = new THREE.ShaderMaterial({
    side: THREE.BackSide,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: {
      uColor: { value: new THREE.Color(0x7dd3fc) }
    },
    vertexShader: `
      varying vec3 vNormal;
      varying vec3 vPosition;
      void main() {
        vNormal = normalize(normalMatrix * normal);
        vPosition = (modelViewMatrix * vec4(position, 1.0)).xyz;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
      }
    `,
    fragmentShader: `
      uniform vec3 uColor;
      varying vec3 vNormal;
      varying vec3 vPosition;
      void main() {
        vec3 viewDir = normalize(-vPosition);
        float rim = 1.0 - max(abs(dot(vNormal, viewDir)), 0.0);
        rim = pow(rim, 3.0);
        gl_FragColor = vec4(uColor, rim * 0.18);
      }
    `
  })
  const glowSphere = new THREE.Mesh(glowGeo, glowMat)
  glowSphere.position.set(0, 2.4, 0)
  scene.add(glowSphere)

  terrainGroup = new THREE.Group()
  scene.add(terrainGroup)

  // ── Terrain Mesh (high-res) ──
  const size = 8
  const seg = 200
  const geo = new THREE.PlaneGeometry(size, size, seg, seg)
  geo.rotateX(-Math.PI / 2)

  const pos = geo.attributes.position
  const colors = new Float32Array(pos.count * 3)

  for (let i = 0; i < pos.count; i++) {
    const x = pos.getX(i)
    const z = pos.getZ(i)
    const y = getHeight(x, z)
    pos.setY(i, y)
    const c = heightToColor(y)
    colors[i * 3] = c.r
    colors[i * 3 + 1] = c.g
    colors[i * 3 + 2] = c.b
  }
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  geo.computeVertexNormals()

  const terrainMat = new THREE.MeshStandardMaterial({
    vertexColors: true,
    side: THREE.DoubleSide,
    roughness: 0.55,
    metalness: 0.05,
    flatShading: false,
    envMapIntensity: 0.4
  })
  terrainGroup.add(new THREE.Mesh(geo, terrainMat))

  // ── Base glow ring ──
  const ringGeo = new THREE.RingGeometry(3.8, 4.5, 64)
  const ringMat = new THREE.MeshBasicMaterial({
    color: 0x7dd3fc,
    transparent: true,
    opacity: 0.06,
    side: THREE.DoubleSide,
    depthWrite: false,
    blending: THREE.AdditiveBlending
  })
  const ring = new THREE.Mesh(ringGeo, ringMat)
  ring.rotation.x = -Math.PI / 2
  ring.position.y = -0.05
  terrainGroup.add(ring)

  // ── Contour lines ──
  const contourGroups = buildContourLines(geo, seg, size)
  const cMat = new THREE.LineBasicMaterial({
    color: 0x475569, transparent: true, opacity: 0.1, depthWrite: false
  })
  contourGroups.forEach(pts => {
    if (pts.length > 2) {
      const g = new THREE.BufferGeometry().setFromPoints(pts)
      terrainGroup.add(new THREE.Line(g, cMat))
    }
  })

  // ── South Col Route Trail ──
  trailCurve = buildTrailCurve()
  const tubeGeo = new THREE.TubeGeometry(trailCurve, 300, 0.02, 6, false)
  const tubeMat = new THREE.MeshBasicMaterial({
    color: 0x0ea5e9, transparent: true, opacity: 0.6, blending: THREE.AdditiveBlending
  })
  terrainGroup.add(new THREE.Mesh(tubeGeo, tubeMat))

  // Trail dots (3 moving dots at different speeds)
  const dotGeo = new THREE.SphereGeometry(0.04, 10, 10)
  const dotColors = [0x0ea5e9, 0x10b981, 0x38bdf8]
  for (let i = 0; i < 3; i++) {
    const dm = new THREE.MeshBasicMaterial({ color: dotColors[i], transparent: true, opacity: 0.9 })
    const dot = new THREE.Mesh(dotGeo, dm)
    // Glow
    const glowMat = new THREE.MeshBasicMaterial({
      color: dotColors[i], transparent: true, opacity: 0.2,
      blending: THREE.AdditiveBlending, depthWrite: false
    })
    dot.add(new THREE.Mesh(new THREE.SphereGeometry(0.1, 8, 8), glowMat))
    terrainGroup.add(dot)
    trailDots.push({ mesh: dot, speed: 0.06 + i * 0.03, offset: i * 0.33 })
  }

  // ── Camp Markers ──
  const camps = [
    { x: -1.2, z: -1.5, label: '5,364m', sub: 'BASE CAMP', flag: '#ef4444' },
    { x: 0.1,  z: -0.1, label: '6,400m', sub: 'CAMP 2',    flag: '#f97316' },
    { x: 0.3,  z: 0.35,  label: '7,920m', sub: 'CAMP 4',    flag: '#eab308' },
    { x: 0.0,  z: 0.0,   label: '8,848m', sub: 'SUMMIT',     flag: '#0ea5e9' },
  ]

  camps.forEach(c => {
    const y = getHeight(c.x, c.z)
    const pinY = y + 0.06
    const side = c.x >= -0.1 ? 1 : -1
    const labelOffX = side * 1.8
    const labelY = pinY + 0.03

    // Pin dot
    const pinMat = new THREE.MeshBasicMaterial({ color: new THREE.Color(c.flag) })
    const pin = new THREE.Mesh(new THREE.SphereGeometry(0.04, 10, 10), pinMat)
    pin.position.set(c.x, pinY, c.z)
    terrainGroup.add(pin)

    // Pin glow
    const pgMat = new THREE.MeshBasicMaterial({
      color: new THREE.Color(c.flag), transparent: true, opacity: 0.15,
      blending: THREE.AdditiveBlending, depthWrite: false
    })
    const pg = new THREE.Mesh(new THREE.SphereGeometry(0.1, 8, 8), pgMat)
    pg.position.copy(pin.position)
    terrainGroup.add(pg)

    // Leader line
    const lineMat = new THREE.LineBasicMaterial({
      color: new THREE.Color(c.flag), transparent: true, opacity: 0.3, depthTest: false
    })
    const lineGeo = new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(c.x, pinY, c.z),
      new THREE.Vector3(c.x + labelOffX, labelY, c.z)
    ])
    terrainGroup.add(new THREE.Line(lineGeo, lineMat))

    // Label
    const lbl = makeLabel(c.label, c.sub, '#0f172a', 'rgba(255,255,255,0.88)')
    lbl.position.set(c.x + labelOffX, labelY, c.z)
    terrainGroup.add(lbl)
  })

  // ── Peak labels (no flag, just name) ──
  const peaks = [
    { x: 0.6, z: -0.7, label: '8,516m', sub: 'LHOTSE' },
    { x: -0.4, z: -1.1, label: '7,861m', sub: 'NUPTSE' },
    { x: -0.9, z: 0.5,  label: '7,583m', sub: 'CHANGTSE' },
  ]
  peaks.forEach(p => {
    const y = getHeight(p.x, p.z)
    const dotY = y + 0.04
    const side = p.x >= 0 ? 1 : -1
    const offX = side * 1.5
    const labelY = dotY + 0.02

    // Small dot
    const dot = new THREE.Mesh(
      new THREE.SphereGeometry(0.03, 8, 8),
      new THREE.MeshBasicMaterial({ color: 0x64748b })
    )
    dot.position.set(p.x, dotY, p.z)
    terrainGroup.add(dot)

    // Leader line
    const lineMat = new THREE.LineBasicMaterial({
      color: 0x64748b, transparent: true, opacity: 0.2, depthTest: false
    })
    const lineGeo = new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(p.x, dotY, p.z),
      new THREE.Vector3(p.x + offX, labelY, p.z)
    ])
    terrainGroup.add(new THREE.Line(lineGeo, lineMat))

    const lbl = makeLabel(p.label, p.sub, '#0f172a', 'rgba(255,255,255,0.78)')
    lbl.position.set(p.x + offX, labelY, p.z)
    terrainGroup.add(lbl)
  })

  // ── Snow Particles ──
  const snowCount = 3000
  const snowPos = new Float32Array(snowCount * 3)
  for (let i = 0; i < snowCount; i++) {
    snowPos[i * 3] = (Math.random() - 0.5) * 10
    snowPos[i * 3 + 1] = Math.random() * 8
    snowPos[i * 3 + 2] = (Math.random() - 0.5) * 10
  }
  const snowGeo = new THREE.BufferGeometry()
  snowGeo.setAttribute('position', new THREE.BufferAttribute(snowPos, 3))
  snowParticles = new THREE.Points(snowGeo, new THREE.PointsMaterial({
    size: 0.025, color: 0xffffff, transparent: true, opacity: 0.55,
    blending: THREE.AdditiveBlending, depthWrite: false, sizeAttenuation: true
  }))
  scene.add(snowParticles)

  startTime = performance.now()
  animate()
}

function animate() {
  animationId = requestAnimationFrame(animate)
  const elapsed = (performance.now() - startTime) / 1000

  // Camera slow orbit
  const camDuration = 3
  const camProgress = Math.min(elapsed / camDuration, 1)
  const eased = 1 - Math.pow(1 - camProgress, 3)
  const orbitAngle = elapsed * 0.08
  const dist = orbitDistance + (1 - eased) * 6
  const baseY = orbitHeight + Math.sin(elapsed * 0.1) * 0.3
  camera.position.set(
    Math.sin(orbitAngle) * dist * 0.7,
    baseY - (1 - eased) * 3,
    Math.cos(orbitAngle) * dist * 0.7
  )
  camera.lookAt(orbitTarget)

  // Trail dots
  if (trailCurve) {
    trailDots.forEach(td => {
      const t = ((elapsed * td.speed + td.offset) % 1)
      td.mesh.position.copy(trailCurve.getPoint(t))
    })
  }

  // Snow
  if (snowParticles) {
    const sp = snowParticles.geometry.attributes.position
    for (let i = 0; i < sp.count; i++) {
      sp.array[i * 3 + 1] -= 0.008
      sp.array[i * 3] += Math.sin(elapsed * 0.5 + i * 0.3) * 0.001
      if (sp.array[i * 3 + 1] < -0.5) sp.array[i * 3 + 1] = 8
    }
    sp.needsUpdate = true
  }

  renderer.render(scene, camera)
}

function onResize() {
  const container = containerRef.value
  if (!container || !camera || !renderer) return
  const w = container.clientWidth
  const h = container.clientHeight
  if (w === 0 || h === 0) return
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

onMounted(() => {
  init()
  resizeObserver = new ResizeObserver(() => onResize())
  if (containerRef.value) resizeObserver.observe(containerRef.value)
  window.addEventListener('resize', onResize)

  // Mouse wheel zoom
  const onWheel = (e) => {
    e.preventDefault()
    orbitDistance = Math.max(12, Math.min(40, orbitDistance + e.deltaY * 0.015))
  }
  containerRef.value.addEventListener('wheel', onWheel, { passive: false })
  if (containerRef.value) containerRef.value._wheelHandler = onWheel
  document.addEventListener('fullscreenchange', onFSChange)
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  document.removeEventListener('fullscreenchange', onFSChange)
  if (resizeObserver) resizeObserver.disconnect()
  const el = containerRef.value
  if (el && el._wheelHandler) {
    el.removeEventListener('wheel', el._wheelHandler)
  }
  if (renderer) {
    renderer.dispose()
    if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement)
  }
  scene?.traverse(obj => {
    if (obj.isMesh || obj.isPoints || obj.isLine || obj.isSprite) {
      obj.geometry?.dispose()
      if (obj.material?.map) obj.material.map.dispose()
      if (Array.isArray(obj.material)) obj.material.forEach(m => { m.map?.dispose(); m.dispose() })
      else obj.material?.dispose()
    }
  })
  scene = null; camera = null; renderer = null
  trailCurve = null; trailDots = []; snowParticles = null
})
</script>

<style scoped>
.alpine-terrain {
  width: 100%;
  height: 100%;
  position: relative;
}
.alpine-terrain canvas {
  display: block;
}
.fullscreen-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid rgba(14,165,233,0.25);
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(8px);
  color: #0ea5e9;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  padding: 0;
}
.fullscreen-btn:hover {
  background: rgba(255,255,255,0.95);
  border-color: rgba(14,165,233,0.5);
  box-shadow: 0 2px 8px rgba(14,165,233,0.15);
}
.fullscreen-btn svg {
  width: 16px;
  height: 16px;
}
</style>
