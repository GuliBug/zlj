<template>
  <div class="g-media" :style="{ height }">
    <img v-if="src && !broken" :src="src" alt="" @error="broken = true" />
    <span v-else class="g-char" :style="{ fontSize: charSize }">{{ char }}</span>
    <slot></slot>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

/**
 * 商品媒体块：image 有值且加载成功显示 <img>；
 * 为空或加载失败回退「渐变红底 + 楷体首字」，绝不出现破图图标。
 */
const props = defineProps({
  image: { type: String, default: '' },
  name: { type: String, default: '' },
  height: { type: String, default: '110px' },
  charSize: { type: String, default: '34px' }
})
const broken = ref(false)
const src = computed(() => (props.image || '').trim())
const char = computed(() => (props.name || '隆').trim().charAt(0) || '隆')
watch(src, () => { broken.value = false })
</script>
