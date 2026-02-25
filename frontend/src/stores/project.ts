import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getProject } from '@/api/project'
import type { Project } from '@/types/api'

export const useProjectStore = defineStore('project', () => {
  const current = ref<Project | null>(null)
  const loading = ref(false)

  async function load(id: number) {
    loading.value = true
    try {
      current.value = await getProject(id)
    } finally {
      loading.value = false
    }
  }

  function clear() {
    current.value = null
  }

  return { current, loading, load, clear }
})
