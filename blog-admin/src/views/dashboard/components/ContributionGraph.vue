<template>
  <div class="contribution-graph">
    <div class="graph-head">
      <div class="graph-title">本年文章发布分布</div>
      <div class="graph-subtitle">{{ currentYear }} 年 1 月 1 日至今天</div>
    </div>

    <div class="months" :style="monthsGridStyle">
      <span
        v-for="item in monthLabels"
        :key="item.label"
        class="month-label"
        :style="{ gridColumnStart: item.weekIndex + 1 }"
      >
        {{ item.label }}
      </span>
    </div>

    <div class="graph-wrapper">
      <div class="weekdays">
        <span>周日</span>
        <span>周一</span>
        <span>周二</span>
        <span>周三</span>
        <span>周四</span>
        <span>周五</span>
        <span>周六</span>
      </div>
      <div class="graph">
        <div
          v-for="(week, weekIndex) in weeklyData"
          :key="weekIndex"
          class="week"
        >
          <div
            v-for="(day, dayIndex) in week"
            :key="dayIndex"
            class="day"
            :class="getActivityClass(day.count)"
            :data-empty="day.empty"
            :title="day.date ? `${formatDate(day.date)} · 发布 ${day.count} 篇` : ''"
          />
        </div>
      </div>
    </div>

    <div class="legend">
      <span>较少</span>
      <div
        v-for="level in 5"
        :key="level"
        class="day"
        :class="`activity-${level - 1}`"
      />
      <span>较多</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

dayjs.locale('zh-cn')

interface DayData {
  date: string
  count: number
  empty?: boolean
}

const props = defineProps<{
  data: DayData[]
}>()

const today = computed(() => dayjs())
const currentYear = computed(() => today.value.year())
const startDate = computed(() => dayjs().startOf('year'))
const startOffset = computed(() => startDate.value.day())

const dataMap = computed(() => {
  return new Map((props.data || []).map(item => [item.date, Number(item.count || 0)]))
})

const calendarDays = computed(() => {
  const result: DayData[] = []
  let cursor = startDate.value

  while (cursor.isBefore(today.value, 'day') || cursor.isSame(today.value, 'day')) {
    const date = cursor.format('YYYY-MM-DD')
    result.push({
      date,
      count: dataMap.value.get(date) ?? 0,
    })
    cursor = cursor.add(1, 'day')
  }

  return result
})

const weeklyData = computed(() => {
  const weeks: DayData[][] = []
  let week: DayData[] = []

  for (let i = 0; i < startOffset.value; i++) {
    week.push({ date: '', count: 0, empty: true })
  }

  calendarDays.value.forEach((day) => {
    week.push(day)
    if (week.length === 7) {
      weeks.push(week)
      week = []
    }
  })

  if (week.length > 0) {
    while (week.length < 7) {
      week.push({ date: '', count: 0, empty: true })
    }
    weeks.push(week)
  }

  return weeks
})

const monthLabels = computed(() => {
  const labels: Array<{ label: string; weekIndex: number }> = []
  let cursor = startDate.value.startOf('month')

  while (cursor.isBefore(today.value, 'month') || cursor.isSame(today.value, 'month')) {
    const diffDays = cursor.diff(startDate.value, 'day')
    const weekIndex = Math.floor((diffDays + startOffset.value) / 7)
    labels.push({
      label: `${cursor.month() + 1}月`,
      weekIndex,
    })
    cursor = cursor.add(1, 'month')
  }

  return labels
})

const monthsGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(weeklyData.value.length, 1)}, minmax(0, 1fr))`,
}))

const maxCount = computed(() => {
  const counts = calendarDays.value.map(item => item.count)
  return counts.length ? Math.max(...counts) : 0
})

const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY年M月D日 dddd')
}

const getActivityLevel = (value: number) => {
  if (value <= 0) {
    return 0
  }

  if (maxCount.value <= 4) {
    return Math.min(4, value)
  }

  const ratio = value / maxCount.value
  if (ratio <= 0.25) {
    return 1
  }
  if (ratio <= 0.5) {
    return 2
  }
  if (ratio <= 0.75) {
    return 3
  }
  return 4
}

const getActivityClass = (value: number) => {
  return `activity-${getActivityLevel(value)}`
}
</script>

<style scoped>
.contribution-graph {
  padding: 20px;
  font-size: 14px;
}

.graph-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}

.graph-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.graph-subtitle {
  font-size: 12px;
  color: #909399;
}

.months {
  display: grid;
  margin-bottom: 15px;
  color: #666;
  padding: 0 15px 0 0;
  margin-left: 20px;
  width: calc(100% - 30px);
}

.month-label {
  text-align: left;
  white-space: nowrap;
}

.graph-wrapper {
  display: flex;
  gap: 10px;
}

.weekdays {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 15px 0;
  color: #666;
  font-size: 12px;
}

.weekdays span {
  height: 15px;
  line-height: 15px;
  margin-bottom: 4px;
}

.graph {
  flex: 1;
  display: flex;
  gap: 4px;
  width: calc(100% - 30px);
  padding: 15px 0;
}

.week {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.day {
  width: 100%;
  aspect-ratio: 1;
  max-width: 15px;
  height: 15px;
  border-radius: 3px;
  background-color: #ebedf0;
  cursor: pointer;
  transition: transform 0.1s ease;
}

.day[data-empty="true"] {
  visibility: hidden;
}

.day:hover {
  transform: scale(1.2);
}

.activity-0 {
  background-color: #ebedf0;
}

.activity-1 {
  background-color: #9be9a8;
}

.activity-2 {
  background-color: #40c463;
}

.activity-3 {
  background-color: #30a14e;
}

.activity-4 {
  background-color: #216e39;
}

.legend {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  justify-content: flex-end;
  color: #666;
  font-size: 14px;
}

@media (prefers-color-scheme: dark) {
  .graph-title {
    color: #e6e6e6;
  }

  .graph-subtitle {
    color: #a8abb2;
  }

  .activity-0 {
    background-color: #161b22;
  }

  .activity-1 {
    background-color: #0e4429;
  }

  .activity-2 {
    background-color: #006d32;
  }

  .activity-3 {
    background-color: #26a641;
  }

  .activity-4 {
    background-color: #39d353;
  }
}
</style>
