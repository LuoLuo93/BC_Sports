<template>
  <div ref="containerRef" class="outdoor-model">
    <button class="fullscreen-btn" @click="toggleFullscreen" title="全屏">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path v-if="!isFullscreen" d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
        <path v-else d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
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
let modelGroup
let dustParticles = null
let animationId = null
let resizeObserver = null
let startTime = 0

// Camera orbit state
let orbitAngle = 0.3
let orbitDistance = 5.5
let orbitHeight = 1.6
const orbitTarget = new THREE.Vector3(0, 1.2, 0)
let isDragging = false
let lastMouse = { x: 0, y: 0 }

// ─── Materials ──────────────────────────────
const materials = {
  // 冲锋衣 — 深蓝防风面料
  jacket: new THREE.MeshStandardMaterial({
    color: 0x1a3a5c,
    roughness: 0.65,
    metalness: 0.05,
  }),
  // 冲锋衣亮色点缀 (拉链/Logo)
  jacketAccent: new THREE.MeshStandardMaterial({
    color: 0xf59e0b,
    roughness: 0.4,
    metalness: 0.1,
  }),
  // 内层保暖
  inner: new THREE.MeshStandardMaterial({
    color: 0x2d4a6f,
    roughness: 0.8,
    metalness: 0.0,
  }),
  // 户外裤 — 灰黑色
  pants: new THREE.MeshStandardMaterial({
    color: 0x2f2f2f,
    roughness: 0.7,
    metalness: 0.02,
  }),
  // 徒步鞋 — 棕色皮革
  boots: new THREE.MeshStandardMaterial({
    color: 0x5c3a1e,
    roughness: 0.55,
    metalness: 0.08,
  }),
  // 鞋底 — 橡胶黑
  sole: new THREE.MeshStandardMaterial({
    color: 0x1a1a1a,
    roughness: 0.9,
    metalness: 0.0,
  }),
  // 背包
  backpack: new THREE.MeshStandardMaterial({
    color: 0xc2410c,
    roughness: 0.6,
    metalness: 0.05,
  }),
  backpackStrap: new THREE.MeshStandardMaterial({
    color: 0x1a1a1a,
    roughness: 0.7,
    metalness: 0.02,
  }),
  // 皮肤
  skin: new THREE.MeshStandardMaterial({
    color: 0xd4a574,
    roughness: 0.8,
    metalness: 0.0,
  }),
  // 头发
  hair: new THREE.MeshStandardMaterial({
    color: 0x2c1810,
    roughness: 0.9,
    metalness: 0.0,
  }),
  // 手套
  gloves: new THREE.MeshStandardMaterial({
    color: 0x1a1a1a,
    roughness: 0.6,
    metalness: 0.05,
  }),
  // 帽子
  hat: new THREE.MeshStandardMaterial({
    color: 0xc2410c,
    roughness: 0.7,
    metalness: 0.0,
  }),
  // 地面
  ground: new THREE.MeshStandardMaterial({
    color: 0x1a2a1a,
    roughness: 0.95,
    metalness: 0.0,
  }),
  // 石头
  rock: new THREE.MeshStandardMaterial({
    color: 0x4a4a4a,
    roughness: 0.85,
    metalness: 0.05,
  }),
}

// ─── Build human figure ──────────────────────
function buildModel() {
  modelGroup = new THREE.Group()

  // ── Torso (冲锋衣) ──
  const torsoGeo = new THREE.CylinderGeometry(0.28, 0.32, 0.75, 16)
  const torso = new THREE.Mesh(torsoGeo, materials.jacket)
  torso.position.y = 1.25
  torso.castShadow = true
  modelGroup.add(torso)

  // Shoulder area (wider)
  const shoulderGeo = new THREE.CylinderGeometry(0.34, 0.28, 0.15, 16)
  const shoulders = new THREE.Mesh(shoulderGeo, materials.jacket)
  shoulders.position.y = 1.6
  shoulders.castShadow = true
  modelGroup.add(shoulders)

  // Collar
  const collarGeo = new THREE.CylinderGeometry(0.18, 0.2, 0.12, 16)
  const collar = new THREE.Mesh(collarGeo, materials.inner)
  collar.position.y = 1.72
  collar.castShadow = true
  modelGroup.add(collar)

  // Center zipper accent
  const zipperGeo = new THREE.BoxGeometry(0.02, 0.7, 0.33)
  const zipper = new THREE.Mesh(zipperGeo, materials.jacketAccent)
  zipper.position.set(0, 1.25, 0.16)
  modelGroup.add(zipper)

  // Chest pocket accent line
  const pocketGeo = new THREE.BoxGeometry(0.15, 0.008, 0.005)
  for (const side of [-1, 1]) {
    const pocket = new THREE.Mesh(pocketGeo, materials.jacketAccent)
    pocket.position.set(side * 0.12, 1.45, 0.325)
    modelGroup.add(pocket)
  }

  // ── Head ──
  const headGeo = new THREE.SphereGeometry(0.16, 16, 16)
  const head = new THREE.Mesh(headGeo, materials.skin)
  head.position.y = 1.95
  head.scale.set(1, 1.1, 1)
  head.castShadow = true
  modelGroup.add(head)

  // Hair
  const hairGeo = new THREE.SphereGeometry(0.165, 16, 16, 0, Math.PI * 2, 0, Math.PI * 0.55)
  const hair = new THREE.Mesh(hairGeo, materials.hair)
  hair.position.y = 1.97
  hair.scale.set(1, 1.1, 1)
  modelGroup.add(hair)

  // 户外帽
  const hatBrimGeo = new THREE.CylinderGeometry(0.22, 0.24, 0.02, 16)
  const hatBrim = new THREE.Mesh(hatBrimGeo, materials.hat)
  hatBrim.position.y = 2.08
  modelGroup.add(hatBrim)

  const hatTopGeo = new THREE.CylinderGeometry(0.14, 0.17, 0.1, 16)
  const hatTop = new THREE.Mesh(hatTopGeo, materials.hat)
  hatTop.position.y = 2.12
  modelGroup.add(hatTop)

  // ── Arms (冲锋衣袖子) ──
  for (const side of [-1, 1]) {
    // Upper arm
    const upperArmGeo = new THREE.CylinderGeometry(0.1, 0.09, 0.35, 12)
    const upperArm = new THREE.Mesh(upperArmGeo, materials.jacket)
    upperArm.position.set(side * 0.4, 1.45, 0)
    upperArm.rotation.z = side * 0.25
    upperArm.castShadow = true
    modelGroup.add(upperArm)

    // Lower arm
    const lowerArmGeo = new THREE.CylinderGeometry(0.09, 0.08, 0.32, 12)
    const lowerArm = new THREE.Mesh(lowerArmGeo, materials.jacket)
    lowerArm.position.set(side * 0.5, 1.18, 0.05)
    lowerArm.rotation.z = side * 0.15
    lowerArm.castShadow = true
    modelGroup.add(lowerArm)

    // Wrist cuff accent
    const cuffGeo = new THREE.CylinderGeometry(0.095, 0.095, 0.03, 12)
    const cuff = new THREE.Mesh(cuffGeo, materials.jacketAccent)
    cuff.position.set(side * 0.52, 1.03, 0.05)
    cuff.rotation.z = side * 0.15
    modelGroup.add(cuff)

    // 手套
    const gloveGeo = new THREE.SphereGeometry(0.065, 12, 12)
    const glove = new THREE.Mesh(gloveGeo, materials.gloves)
    glove.position.set(side * 0.54, 0.98, 0.05)
    glove.scale.set(1, 0.85, 1)
    modelGroup.add(glove)
  }

  // ── Backpack ──
  const packBodyGeo = new THREE.BoxGeometry(0.38, 0.45, 0.18)
  const packBody = new THREE.Mesh(packBodyGeo, materials.backpack)
  packBody.position.set(0, 1.35, -0.22)
  packBody.castShadow = true
  modelGroup.add(packBody)

  // Pack top flap
  const flapGeo = new THREE.BoxGeometry(0.36, 0.08, 0.19)
  const flap = new THREE.Mesh(flapGeo, materials.backpack)
  flap.position.set(0, 1.6, -0.22)
  modelGroup.add(flap)

  // Pack straps
  for (const side of [-1, 1]) {
    const strapGeo = new THREE.BoxGeometry(0.04, 0.5, 0.02)
    const strap = new THREE.Mesh(strapGeo, materials.backpackStrap)
    strap.position.set(side * 0.14, 1.35, -0.12)
    strap.rotation.x = 0.15
    modelGroup.add(strap)
  }

  // Pack front pocket
  const frontPocketGeo = new THREE.BoxGeometry(0.25, 0.18, 0.03)
  const frontPocket = new THREE.Mesh(frontPocketGeo, materials.backpackStrap)
  frontPocket.position.set(0, 1.2, -0.33)
  modelGroup.add(frontPocket)

  // ── Hiking belt ──
  const beltGeo = new THREE.TorusGeometry(0.31, 0.02, 8, 24)
  const belt = new THREE.Mesh(beltGeo, materials.backpackStrap)
  belt.position.y = 0.92
  belt.rotation.x = Math.PI / 2
  modelGroup.add(belt)

  // Belt buckle
  const buckleGeo = new THREE.BoxGeometry(0.06, 0.04, 0.04)
  const buckle = new THREE.Mesh(buckleGeo, materials.jacketAccent)
  buckle.position.set(0, 0.92, 0.32)
  modelGroup.add(buckle)

  // ── Pants (户外裤) ──
  for (const side of [-1, 1]) {
    // Hip
    const hipGeo = new THREE.CylinderGeometry(0.14, 0.12, 0.2, 12)
    const hip = new THREE.Mesh(hipGeo, materials.pants)
    hip.position.set(side * 0.14, 0.78, 0)
    hip.castShadow = true
    modelGroup.add(hip)

    // Upper leg
    const upperLegGeo = new THREE.CylinderGeometry(0.12, 0.1, 0.3, 12)
    const upperLeg = new THREE.Mesh(upperLegGeo, materials.pants)
    upperLeg.position.set(side * 0.15, 0.5, 0)
    upperLeg.castShadow = true
    modelGroup.add(upperLeg)

    // Knee patch accent
    const kneeGeo = new THREE.BoxGeometry(0.08, 0.08, 0.005)
    const knee = new THREE.Mesh(kneeGeo, materials.jacketAccent)
    knee.position.set(side * 0.15, 0.45, 0.105)
    modelGroup.add(knee)

    // Lower leg
    const lowerLegGeo = new THREE.CylinderGeometry(0.1, 0.09, 0.3, 12)
    const lowerLeg = new THREE.Mesh(lowerLegGeo, materials.pants)
    lowerLeg.position.set(side * 0.15, 0.2, 0)
    lowerLeg.castShadow = true
    modelGroup.add(lowerLeg)
  }

  // ── Hiking boots (徒步鞋) ──
  for (const side of [-1, 1]) {
    // Boot shaft
    const shaftGeo = new THREE.CylinderGeometry(0.1, 0.11, 0.12, 12)
    const shaft = new THREE.Mesh(shaftGeo, materials.boots)
    shaft.position.set(side * 0.15, 0.02, 0)
    shaft.castShadow = true
    modelGroup.add(shaft)

    // Boot body
    const bootGeo = new THREE.BoxGeometry(0.2, 0.08, 0.28)
    const boot = new THREE.Mesh(bootGeo, materials.boots)
    boot.position.set(side * 0.15, -0.04, 0.03)
    boot.castShadow = true
    modelGroup.add(boot)

    // Sole (thick rubber)
    const soleGeo = new THREE.BoxGeometry(0.22, 0.04, 0.32)
    const sole = new THREE.Mesh(soleGeo, materials.sole)
    sole.position.set(side * 0.15, -0.09, 0.03)
    sole.castShadow = true
    modelGroup.add(sole)

    // Boot laces
    const laceGeo = new THREE.BoxGeometry(0.015, 0.08, 0.005)
    for (let i = 0; i < 3; i++) {
      const lace = new THREE.Mesh(laceGeo, materials.jacketAccent)
      lace.position.set(side * 0.15, 0.02 + i * 0.025, 0.145)
      modelGroup.add(lace)
    }
  }

  // ── Carabiner on belt ──
  const carabinerGeo = new THREE.TorusGeometry(0.025, 0.005, 8, 12)
  const carabiner = new THREE.Mesh(carabinerGeo, materials.jacketAccent)
  carabiner.position.set(-0.3, 0.88, 0.1)
  modelGroup.add(carabiner)

  scene.add(modelGroup)
}

// ─── Ground & environment ────────────────────
function buildEnvironment() {
  // Ground plane with subtle grid texture
  const groundGeo = new THREE.CircleGeometry(4, 64)
  const ground = new THREE.Mesh(groundGeo, materials.ground)
  ground.rotation.x = -Math.PI / 2
  ground.position.y = -0.11
  ground.receiveShadow = true
  scene.add(ground)

  // Ground ring (subtle highlight)
  const ringGeo = new THREE.RingGeometry(1.2, 1.25, 64)
  const ringMat = new THREE.MeshBasicMaterial({ color: 0x0ea5e9, transparent: true, opacity: 0.15 })
  const ring = new THREE.Mesh(ringGeo, ringMat)
  ring.rotation.x = -Math.PI / 2
  ring.position.y = -0.1
  scene.add(ring)

  // Decorative rocks
  const rockPositions = [
    { x: 1.2, z: 0.5, s: 0.15 },
    { x: -1.0, z: 0.8, s: 0.1 },
    { x: 0.8, z: -0.7, s: 0.12 },
    { x: -1.3, z: -0.3, s: 0.08 },
  ]
  rockPositions.forEach(p => {
    const geo = new THREE.DodecahedronGeometry(p.s, 1)
    const rock = new THREE.Mesh(geo, materials.rock)
    rock.position.set(p.x, p.s * 0.3 - 0.1, p.z)
    rock.rotation.set(Math.random(), Math.random(), Math.random())
    rock.castShadow = true
    scene.add(rock)
  })

  // Floating dust particles
  const particleCount = 120
  const pGeo = new THREE.BufferGeometry()
  const positions = new Float32Array(particleCount * 3)
  for (let i = 0; i < particleCount; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 6
    positions[i * 3 + 1] = Math.random() * 4
    positions[i * 3 + 2] = (Math.random() - 0.5) * 6
  }
  pGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const pMat = new THREE.PointsMaterial({
    color: 0xffffff,
    size: 0.015,
    transparent: true,
    opacity: 0.25,
    blending: THREE.AdditiveBlending,
  })
  dustParticles = new THREE.Points(pGeo, pMat)
  dustParticles.userData.isParticles = true
  scene.add(dustParticles)
}

// ─── Lighting ────────────────────────────────
function setupLighting() {
  // Ambient
  scene.add(new THREE.AmbientLight(0x4488cc, 0.6))

  // Key light (warm, from front-right)
  const keyLight = new THREE.DirectionalLight(0xffeedd, 1.2)
  keyLight.position.set(3, 5, 4)
  keyLight.castShadow = true
  keyLight.shadow.mapSize.set(1024, 1024)
  keyLight.shadow.camera.near = 0.5
  keyLight.shadow.camera.far = 15
  keyLight.shadow.camera.left = -3
  keyLight.shadow.camera.right = 3
  keyLight.shadow.camera.top = 4
  keyLight.shadow.camera.bottom = -1
  scene.add(keyLight)

  // Fill light (cool, from left)
  const fillLight = new THREE.DirectionalLight(0x88bbff, 0.5)
  fillLight.position.set(-3, 3, -2)
  scene.add(fillLight)

  // Rim light (back)
  const rimLight = new THREE.DirectionalLight(0x0ea5e9, 0.6)
  rimLight.position.set(0, 3, -5)
  scene.add(rimLight)

  // Point light accent (warm glow at feet)
  const pointLight = new THREE.PointLight(0xf59e0b, 0.3, 4)
  pointLight.position.set(0, 0.3, 1)
  scene.add(pointLight)
}

// ─── Scene setup ─────────────────────────────
function initScene() {
  const container = containerRef.value
  if (!container) return

  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x0a1628, 0.08)

  camera = new THREE.PerspectiveCamera(42, container.clientWidth / container.clientHeight, 0.1, 100)
  updateCamera()

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(container.clientWidth, container.clientHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.1
  container.appendChild(renderer.domElement)

  setupLighting()
  buildModel()
  buildEnvironment()

  startTime = performance.now()
  animate()

  // Resize
  resizeObserver = new ResizeObserver(() => {
    const w = container.clientWidth
    const h = container.clientHeight
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
  })
  resizeObserver.observe(container)

  // Mouse orbit
  renderer.domElement.addEventListener('mousedown', onMouseDown)
  renderer.domElement.addEventListener('mousemove', onMouseMove)
  renderer.domElement.addEventListener('mouseup', onMouseUp)
  renderer.domElement.addEventListener('mouseleave', onMouseUp)
  renderer.domElement.addEventListener('wheel', onWheel, { passive: true })
}

function updateCamera() {
  camera.position.set(
    Math.sin(orbitAngle) * orbitDistance,
    orbitHeight,
    Math.cos(orbitAngle) * orbitDistance
  )
  camera.lookAt(orbitTarget)
}

function onMouseDown(e) {
  isDragging = true
  lastMouse.x = e.clientX
  lastMouse.y = e.clientY
}

function onMouseMove(e) {
  if (!isDragging) return
  const dx = e.clientX - lastMouse.x
  const dy = e.clientY - lastMouse.y
  orbitAngle -= dx * 0.005
  orbitHeight = Math.max(0.5, Math.min(4, orbitHeight + dy * 0.01))
  lastMouse.x = e.clientX
  lastMouse.y = e.clientY
  updateCamera()
}

function onMouseUp() {
  isDragging = false
}

function onWheel(e) {
  orbitDistance = Math.max(2.5, Math.min(10, orbitDistance + e.deltaY * 0.005))
  updateCamera()
}

// ─── Animation loop ──────────────────────────
function animate() {
  animationId = requestAnimationFrame(animate)
  const t = (performance.now() - startTime) * 0.001

  // Gentle model breathing / sway
  if (modelGroup) {
    modelGroup.rotation.y = Math.sin(t * 0.3) * 0.03
    modelGroup.position.y = Math.sin(t * 0.8) * 0.01
  }

  // Animate dust particles
  if (dustParticles?.geometry) {
    const pos = dustParticles.geometry.attributes.position.array
    for (let i = 1; i < pos.length; i += 3) {
      pos[i] += 0.002
      if (pos[i] > 4) pos[i] = 0
    }
    dustParticles.geometry.attributes.position.needsUpdate = true
  }

  // Slow auto-orbit when not dragging
  if (!isDragging) {
    orbitAngle += 0.001
    updateCamera()
  }

  renderer.render(scene, camera)
}

// ─── Lifecycle ───────────────────────────────
onMounted(() => {
  initScene()
  document.addEventListener('fullscreenchange', onFSChange)
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  document.removeEventListener('fullscreenchange', onFSChange)
  if (resizeObserver) resizeObserver.disconnect()
  if (renderer) {
    renderer.domElement.removeEventListener('mousedown', onMouseDown)
    renderer.domElement.removeEventListener('mousemove', onMouseMove)
    renderer.domElement.removeEventListener('mouseup', onMouseUp)
    renderer.domElement.removeEventListener('mouseleave', onMouseUp)
    renderer.domElement.removeEventListener('wheel', onWheel)
    renderer.dispose()
  }
  if (scene) {
    scene.traverse(obj => {
      if (obj.geometry) obj.geometry.dispose()
      if (obj.material) {
        if (Array.isArray(obj.material)) obj.material.forEach(m => m.dispose())
        else obj.material.dispose()
      }
    })
  }
})
</script>

<style scoped>
.outdoor-model {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 12px;
}

.outdoor-model canvas {
  display: block;
}

.fullscreen-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
  width: 32px;
  height: 32px;
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  backdrop-filter: blur(8px);
}

.fullscreen-btn:hover {
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
}

.fullscreen-btn svg {
  width: 16px;
  height: 16px;
}
</style>
