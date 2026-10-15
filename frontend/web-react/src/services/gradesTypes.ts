import type { ReportClassroom, ReportRoute } from './reportsApi'
export interface GradeAverage { value: number; sortValue: number; letter: 'AD' | 'A' | 'B' | 'C' }
export interface GradeDetail { attemptId: string; activityId: string; title: string; date: string; earned: number; possible: number; grade: number }
export interface GradeCell { unitId: string; average: GradeAverage | null; totalActivities: number; details: GradeDetail[] }
export interface GradeRow { studentId: string; studentName: string; cells: GradeCell[] }
export interface GradesReport { classroom: ReportClassroom; route: ReportRoute; generatedAt: string
  units: { id: string; title: string }[]; students: GradeRow[]; unassignedNodes: number }
