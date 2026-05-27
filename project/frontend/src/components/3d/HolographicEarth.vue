<template>
  <div ref="containerRef" class="holographic-earth"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'

const containerRef = ref(null)

let scene, camera, renderer
let mountainGroup, snowParticles, auroraMesh
let contourRings = [], trailDots = [], skiTrails = []
let animationId = null
let resizeObserver = null
let startTime = 0

const ICE = 0x4FC3F7
const SNOW = 0xE8F0FE
const SUNSET = 0xE8602C
const PINE = 0x2E7D32
const AURORA_G = 0x00E676
const AURORA_P = 0x7C4DFF
const S = 0.55

function init() {
  const container = containerRef.value
  if (!container) return
  const w = container.clientWidth
  const h = container.clientHeight
  if (w === 0 || h === 0) return

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 1000)
  camera.position.set(0, 4 * S, 26 * S)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'high-performance' })
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2))
  renderer.setSize(w, h)
  container.appendChild(renderer.domElement)

  mountainGroup = new THREE.Group()
  mountainGroup.scale.setScalar(S)
  scene.add(mountainGroup)

  // ═══════════════════════════════════════════
  // 1. Mountain Peaks (低多边形雪山群)
  // ═══════════════════════════════════════════
  const peaks = [
    { h: 12, r: 6, x: 0, z: 0 },       // 主峰
    { h: 9, r: 5, x: -5, z: 2 },       // 左侧峰
    { h: 7, r: 4.5, x: 4, z: 3 },      // 右前峰
    { h: 8, r: 4, x: 2, z: -4 },       // 右后峰
    { h: 5, r: 3.5, x: -3, z: -3 },    // 左后小峰
  ]

  const rockMat = new THREE.MeshBasicMaterial({ color: 0x1a2a3a, transparent: true, opacity: 0.85 })
  const edgeMat = new THREE.LineBasicMaterial({ color: ICE, transparent: true, opacity: 0.35, blending: THREE.AdditiveBlending })

  peaks.forEach(pk => {
    const coneGeo = new THREE.ConeGeometry(pk.r, pk.h, 6, 1)
    const cone = new THREE.Mesh(coneGeo, rockMat)
    cone.position.set(pk.x, pk.h / 2 - 4, pk.z)
    mountainGroup.add(cone)

    // 发光棱线
    const edges = new THREE.LineSegments(new THREE.EdgesGeometry(coneGeo), edgeMat)
    edges.position.copy(cone.position)
    mountainGroup.add(edges)

    // 雪顶 (smaller white cone on top)
    const snowCapH = pk.h * 0.3
    const snowCapR = pk.r * 0.5
    const snowGeo = new THREE.ConeGeometry(snowCapR, snowCapH, 6, 1)
    const snowMat = new THREE.MeshBasicMaterial({
      color: SNOW, transparent: true, opacity: 0.3,
      blending: THREE.AdditiveBlending, depthWrite: false
    })
    const snowCap = new THREE.Mesh(snowGeo, snowMat)
    snowCap.position.set(pk.x, pk.h - snowCapH / 2 - 4, pk.z)
    mountainGroup.add(snowCap)

    // 雪顶棱线
    const snowEdges = new THREE.LineSegments(
      new THREE.EdgesGeometry(snowGeo),
      new THREE.LineBasicMaterial({ color: SNOW, transparent: true, opacity: 0.5, blending: THREE.AdditiveBlending })
    )
    snowEdges.position.copy(snowCap.position)
    mountainGroup.add(snowEdges)
  })

  // ═══════════════════════════════════════════
  // 2. Topographic Contour Rings (等高线)
  // ═══════════════════════════════════════════
  const contourMat = new THREE.MeshBasicMaterial({
    color: ICE, transparent: true, opacity: 0.1,
    blending: THREE.AdditiveBlending, depthWrite: false
  })
  const heights = [-3, -1, 1, 3, 5, 7]
  heights.forEach((y, i) => {
    const r = 7 - i * 0.8
    const ring = new THREE.Mesh(new THREE.TorusGeometry(r, 0.02, 4, 64), contourMat)
    ring.rotation.x = Math.PI / 2
    ring.position.y = y
    mountainGroup.add(ring)
    contourRings.push(ring)
  })

  // ═══════════════════════════════════════════
  // 3. Ski Trails (滑雪道)
  // ═══════════════════════════════════════════
  const trailColors = [
    { color: 0x4CAF50, name: 'beginner' },   // 绿道 初级
    { color: 0x2196F3, name: 'intermediate' }, // 蓝道 中级
    { color: 0xE8602C, name: 'advanced' },     // 红道 高级
    { color: 0x212121, name: 'expert' },       // 黑道 专家
  ]

  const trailCurveMat = new THREE.MeshBasicMaterial({
    color: ICE, transparent: true, opacity: 0.2,
    blending: THREE.AdditiveBlending, depthWrite: false
  })
  const dotGeo = new THREE.SphereGeometry(0.15, 8, 8)

  const trailDefs = [
    { start: [0, 7, 0], ctrl1: [2, 5, 1], ctrl2: [3, 2, 2], end: [4, -3, 3], colorIdx: 1 },
    { start: [0, 7, 0], ctrl1: [-1, 4, 2], ctrl2: [-3, 1, 1], end: [-4, -3, 2], colorIdx: 2 },
    { start: [-4, 5, 2], ctrl1: [-3, 3, 3], ctrl2: [-1, 0, 4], end: [1, -3, 5], colorIdx: 0 },
    { start: [2, 5, 3], ctrl1: [3, 2, 2], ctrl2: [4, 0, 0], end: [5, -3, -1], colorIdx: 3 },
    { start: [0, 7, 0], ctrl1: [0, 4, -2], ctrl2: [1, 1, -3], end: [2, -3, -4], colorIdx: 1 },
    { start: [-3, 4, -2], ctrl1: [-2, 1, -1], ctrl2: [0, -1, 0], end: [2, -3, 1], colorIdx: 0 },
  ]

  trailDefs.forEach(td => {
    const curve = new THREE.CubicBezierCurve3(
      new THREE.Vector3(...td.start),
      new THREE.Vector3(...td.ctrl1),
      new THREE.Vector3(...td.ctrl2),
      new THREE.Vector3(...td.end)
    )
    const tubeGeo = new THREE.TubeGeometry(curve, 30, 0.04, 6, false)
    const tubeMat = new THREE.MeshBasicMaterial({
      color: trailColors[td.colorIdx].color, transparent: true, opacity: 0.3,
      blending: THREE.AdditiveBlending, depthWrite: false
    })
    mountainGroup.add(new THREE.Mesh(tubeGeo, tubeMat))

    // 滑行光点
    const dotMat = new THREE.MeshBasicMaterial({
      color: trailColors[td.colorIdx].color, transparent: true, opacity: 0.9,
      blending: THREE.AdditiveBlending, depthWrite: false
    })
    const dot = new THREE.Mesh(dotGeo, dotMat)
    mountainGroup.add(dot)

    skiTrails.push({
      curve, dot,
      speed: 0.04 + Math.random() * 0.06,
      offset: Math.random()
    })
  })

  // ═══════════════════════════════════════════
  // 4. Snow Particles (雪花)
  // ═══════════════════════════════════════════
  const snowCount = 2000
  const snowPos = new Float32Array(snowCount * 3)
  const snowCol = new Float32Array(snowCount * 3)
  for (let i = 0; i < snowCount; i++) {
    snowPos[i * 3] = (Math.random() - 0.5) * 30
    snowPos[i * 3 + 1] = Math.random() * 20 - 4
    snowPos[i * 3 + 2] = (Math.random() - 0.5) * 30
    snowCol[i * 3] = 0.9; snowCol[i * 3 + 1] = 0.95; snowCol[i * 3 + 2] = 1.0
  }
  const snowGeo = new THREE.BufferGeometry()
  snowGeo.setAttribute('position', new THREE.BufferAttribute(snowPos, 3))
  snowGeo.setAttribute('color', new THREE.BufferAttribute(snowCol, 3))
  snowParticles = new THREE.Points(snowGeo, new THREE.PointsMaterial({
    size: 0.1, vertexColors: true, transparent: true, opacity: 0.6,
    blending: THREE.AdditiveBlending, depthWrite: false, sizeAttenuation: true
  }))
  snowParticles.scale.setScalar(S)
  scene.add(snowParticles)

  // ═══════════════════════════════════════════
  // 5. Aurora Borealis (北极光)
  // ═══════════════════════════════════════════
  const auroraGeo = new THREE.PlaneGeometry(30, 6, 64, 8)
  auroraMesh = new THREE.Mesh(auroraGeo, new THREE.ShaderMaterial({
    uniforms: {
      uTime: { value: 0 },
      uColor1: { value: new THREE.Color('#00E676') },
      uColor2: { value: new THREE.Color('#7C4DFF') },
    },
    vertexShader: `
      uniform float uTime;
      varying vec2 vUv;
      varying float vDisp;
      void main() {
        vUv = uv;
        vec3 pos = position;
        float wave = sin(pos.x * 0.3 + uTime * 0.5) * cos(pos.x * 0.1 + uTime * 0.3);
        pos.y += wave * 1.5;
        pos.z += sin(pos.x * 0.2 + uTime * 0.4) * 2.0;
        vDisp = wave;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(pos, 1.0);
      }
    `,
    fragmentShader: `
      uniform vec3 uColor1;
      uniform vec3 uColor2;
      varying vec2 vUv;
      varying float vDisp;
      void main() {
        float mask = smoothstep(0.0, 0.3, vUv.y) * smoothstep(1.0, 0.5, vUv.y);
        vec3 color = mix(uColor1, uColor2, vUv.x * 0.5 + 0.25);
        float alpha = mask * (0.08 + abs(vDisp) * 0.06);
        gl_FragColor = vec4(color, alpha);
      }
    `,
    transparent: true, blending: THREE.AdditiveBlending,
    depthWrite: false, side: THREE.DoubleSide
  }))
  auroraMesh.position.y = 12
  auroraMesh.scale.setScalar(S)
  scene.add(auroraMesh)

  // ═══════════════════════════════════════════
  // 6. Base Platform (地形底座)
  // ═══════════════════════════════════════════
  const baseGroup = new THREE.Group()
  baseGroup.scale.setScalar(S)
  baseGroup.position.y = -4

  const baseRingMat = new THREE.MeshBasicMaterial({
    color: ICE, transparent: true, opacity: 0.12,
    blending: THREE.AdditiveBlending, depthWrite: false
  });
  [6, 8, 10, 12].forEach(r => {
    const ring = new THREE.Mesh(new THREE.TorusGeometry(r, 0.03, 4, 80), baseRingMat)
    ring.rotation.x = Math.PI / 2
    baseGroup.add(ring)
  })

  // 径向线
  const radMat = new THREE.LineBasicMaterial({ color: ICE, transparent: true, opacity: 0.06, blending: THREE.AdditiveBlending })
  for (let i = 0; i < 12; i++) {
    const a = (i / 12) * Math.PI * 2
    baseGroup.add(new THREE.Line(
      new THREE.BufferGeometry().setFromPoints([
        new THREE.Vector3(0, 0, 0),
        new THREE.Vector3(Math.cos(a) * 12, 0, Math.sin(a) * 12)
      ]), radMat
    ))
  }

  scene.add(baseGroup)

  startTime = performance.now()
  animate()
}

function animate() {
  animationId = requestAnimationFrame(animate)
  const elapsed = (performance.now() - startTime) / 1000

  // Camera entrance
  const camDuration = 3
  const camProgress = Math.min(elapsed / camDuration, 1)
  const eased = 1 - Math.pow(1 - camProgress, 3)
  const finalZ = 26 * S
  camera.position.set(0, 4 * S - eased * 2 * S, finalZ - 12 * S + eased * 12 * S)
  camera.lookAt(0, 0, 0)

  // Mountain slow rotation
  if (mountainGroup) {
    mountainGroup.rotation.y += 0.001
  }

  // Snow falling
  if (snowParticles) {
    const pos = snowParticles.geometry.attributes.position
    for (let i = 0; i < pos.count; i++) {
      pos.array[i * 3 + 1] -= 0.015
      pos.array[i * 3] += Math.sin(elapsed + i) * 0.002
      if (pos.array[i * 3 + 1] < -4) {
        pos.array[i * 3 + 1] = 16
      }
    }
    pos.needsUpdate = true
  }

  // Aurora time uniform
  if (auroraMesh) {
    auroraMesh.material.uniforms.uTime.value = elapsed
  }

  // Ski trail dots
  skiTrails.forEach(st => {
    const t = ((elapsed * st.speed + st.offset) % 1)
    st.dot.position.copy(st.curve.getPoint(t))
  })

  // Contour rings subtle pulse
  contourRings.forEach((ring, i) => {
    ring.material.opacity = 0.08 + Math.sin(elapsed * 0.5 + i * 0.5) * 0.03
  })

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
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  if (resizeObserver) resizeObserver.disconnect()
  if (renderer) {
    renderer.dispose()
    if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement)
  }
  scene?.traverse(obj => {
    if (obj.isMesh || obj.isPoints || obj.isLine) {
      obj.geometry?.dispose()
      if (Array.isArray(obj.material)) obj.material.forEach(m => m.dispose())
      else obj.material?.dispose()
    }
  })
  scene = null; camera = null; renderer = null
  contourRings = []; trailDots = []; skiTrails = []
})
</script>

<style scoped>
.holographic-earth { width: 100%; height: 100%; position: relative; }
.holographic-earth canvas { display: block; }
</style>
