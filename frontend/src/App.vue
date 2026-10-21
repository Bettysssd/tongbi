<script setup>
import { computed, ref } from 'vue'
import { postJson } from './api'

const draft = ref('')
const fileName = ref('')
const showIntake = ref(true)
const analysis = ref(null)
const tab = ref('desk')
const activeIssue = ref('')
const rewrite = ref('')
const busy = ref(false)
const error = ref('')
const answers = ref({})
const quizResult = ref(null)

const score = computed(() => (analysis.value ? analysis.value.score : null))
const issues = computed(() => (analysis.value ? analysis.value.issues : []))
const currentIssue = computed(() => issues.value.find((item) => item.id === activeIssue.value) || issues.value[0] || null)
const listLabel = computed(() => {
  if (!analysis.value) return ''
  return analysis.value.openCount === 0 ? '这一处都处理过了' : '还剩 ' + analysis.value.openCount + ' 条未解决'
})

function applyAnalysis(next) {
  analysis.value = next
  rewrite.value = next.rewrite
  activeIssue.value = next.issues.length ? next.issues[0].id : ''
  answers.value = {}
  quizResult.value = null
  showIntake.value = false
  tab.value = 'desk'
}

async function run(task) {
  busy.value = true
  error.value = ''
  try {
    await task()
  } catch (caught) {
    error.value = caught.message || '请求失败'
  } finally {
    busy.value = false
  }
}

function loadSample() {
  return run(async () => {
    applyAnalysis(await postJson('/api/analyses/sample', {}))
  })
}

function analyze() {
  return run(async () => {
    applyAnalysis(await postJson('/api/analyses', { code: draft.value, fileName: fileName.value }))
  })
}

function rescore() {
  return run(async () => {
    const next = await postJson('/api/analyses/' + analysis.value.id + '/score', { code: rewrite.value })
    const kept = rewrite.value
    analysis.value = next
    rewrite.value = kept
  })
}

function setStatus(id, status) {
  return run(async () => {
    analysis.value = await postJson('/api/analyses/' + analysis.value.id + '/issues/' + id, { status })
    activeIssue.value = id
    tab.value = 'list'
  })
}

function submitQuiz() {
  return run(async () => {
    quizResult.value = await postJson('/api/analyses/' + analysis.value.id + '/quiz', { answers: answers.value })
  })
}

function restore() {
  rewrite.value = analysis.value.suggestion
}
</script>

<template>
  <main class="app">
    <header class="head">
      <div>
        <p class="mark">同笔</p>
        <h1>{{ analysis ? analysis.title : '先贴一个 Java 方法' }}</h1>
        <p class="meta">{{ analysis ? analysis.meta : '规则会按你现在的写法改一处，并给出分数和问题单。' }}</p>
      </div>
      <div v-if="score" class="scorebox">
        <p class="score-label">{{ tab === 'list' ? '清单评分' : tab === 'read' ? '看懂' : '当前评分' }}</p>
        <p class="score">{{ tab === 'list' ? analysis.listScore : tab === 'read' ? (quizResult ? quizResult.scoreText : '—') : score.total }}</p>
        <p v-if="tab === 'desk'" class="metrics">
          <span>正确性 <b>{{ score.correctness }}</b></span>
          <span>可读性 <b>{{ score.readability }}</b></span>
          <span>风格 <b>{{ score.style }}</b></span>
        </p>
      </div>
    </header>

    <div v-if="analysis" class="tabs" role="tablist">
      <button type="button" role="tab" :aria-selected="tab === 'desk'" @click="tab = 'desk'">对照台</button>
      <button type="button" role="tab" :aria-selected="tab === 'list'" @click="tab = 'list'">问题单</button>
      <button type="button" role="tab" :aria-selected="tab === 'read'" @click="tab = 'read'">读码</button>
      <button type="button" @click="showIntake = !showIntake">换一段代码</button>
    </div>

    <section v-if="showIntake || !analysis" class="intake">
      <h2>你的代码</h2>
      <textarea v-model="draft" spellcheck="false" aria-label="要分析的 Java 代码" placeholder="贴一个方法，或一整份类。"></textarea>
      <input v-model="fileName" class="file-name" aria-label="文件名" placeholder="可选文件名，例如 BidItemService.java" />
      <div class="intake-actions">
        <button type="button" class="primary" :disabled="busy || !draft.trim()" @click="analyze">分析这段代码</button>
        <button type="button" :disabled="busy" @click="loadSample">载入示例 filterValid</button>
      </div>
    </section>

    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <section v-if="analysis && tab === 'desk'">
      <div v-if="issues.length" class="chips" role="group" aria-label="这一处要看的问题">
        <button
          v-for="item in issues"
          :key="item.id"
          type="button"
          class="chip"
          :aria-pressed="currentIssue && currentIssue.id === item.id"
          @click="activeIssue = item.id"
        >{{ item.title }}</button>
      </div>
      <p v-else class="hint">这个方法没有命中判空、状态字面量、double 金额这三条。</p>
      <div class="cols">
        <section class="pane" aria-label="你现在的写法">
          <h2>你现在的写法</h2>
          <pre>{{ analysis.originalCode }}</pre>
        </section>
        <section class="pane" aria-label="同风格改写">
          <h2>同风格改写</h2>
          <textarea v-model="rewrite" rows="16" spellcheck="false" aria-label="可修改的同风格改写"></textarea>
        </section>
      </div>
      <div class="actions">
        <button type="button" class="primary" :disabled="busy" @click="rescore">按现在的改写重新评分</button>
        <button type="button" :disabled="busy" @click="restore">恢复建议稿</button>
        <p aria-live="polite">{{ score.note }}{{ analysis.scored ? '' : ' 当前分数还是左边这版。' }}</p>
      </div>
      <dl v-if="currentIssue" class="reasons">
        <div><dt>为什么要换</dt><dd>{{ currentIssue.why }}</dd></div>
        <div><dt>为什么更好</dt><dd>{{ currentIssue.better }}</dd></div>
        <div><dt>常见原因</dt><dd>{{ currentIssue.miss }}</dd></div>
      </dl>
    </section>

    <section v-if="analysis && tab === 'list'">
      <p class="meta">{{ listLabel }}</p>
      <p v-if="!issues.length" class="hint">没有要回顾的问题。</p>
      <article v-for="item in issues" :key="item.id" class="issue">
        <button type="button" class="issue-row" :aria-pressed="activeIssue === item.id" @click="activeIssue = item.id">
          <span class="issue-title">{{ item.title }}</span>
          <span class="status" :class="{ done: item.status === '已改善' }">{{ item.status }}</span>
        </button>
        <div v-if="activeIssue === item.id" class="detail">
          <div class="pair">
            <div>
              <h2>现在</h2>
              <pre>{{ item.before }}</pre>
            </div>
            <div>
              <h2>可更换</h2>
              <pre>{{ item.after }}</pre>
            </div>
          </div>
          <p>为什么要换：{{ item.why }}</p>
          <p>为什么更好：{{ item.better }}</p>
          <p>常见原因：{{ item.miss }}</p>
          <div class="row-actions">
            <button type="button" :disabled="busy" @click="setStatus(item.id, '已改善')">标成已改善</button>
            <button type="button" :disabled="busy" @click="setStatus(item.id, '已忽略')">先忽略</button>
            <button type="button" :disabled="busy" @click="setStatus(item.id, '未解决')">标回未解决</button>
          </div>
        </div>
      </article>
    </section>

    <section v-if="analysis && tab === 'read'">
      <p class="meta">先判断原来的写法会怎样，再看同风格改法。</p>
      <pre class="solo">{{ analysis.originalCode }}</pre>
      <p v-if="!analysis.quiz.length" class="hint">这版没有要核对的读码题。</p>
      <form v-else @submit.prevent="submitQuiz">
        <fieldset v-for="question in analysis.quiz" :key="question.id">
          <legend>{{ question.prompt }}</legend>
          <label v-for="option in question.options" :key="option.value">
            <input v-model="answers[question.id]" type="radio" :name="question.id" :value="option.value" />
            {{ option.label }}
          </label>
        </fieldset>
        <button type="submit" class="primary" :disabled="busy">看这些的结果</button>
      </form>
      <div v-if="quizResult" class="result">
        <p>{{ quizResult.judge }}</p>
        <p>{{ quizResult.truth }}</p>
        <p>{{ quizResult.miss }}</p>
      </div>
    </section>

    <p class="foot">评分来自固定规则，不是模型打分。分析只留在内存里，重启后端就会清空。</p>
  </main>
</template>
