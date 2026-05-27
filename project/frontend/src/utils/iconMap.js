import {
  Odometer,
  Menu as MenuIcon, Setting, User, Key, Lock,
  Document, Folder, FolderOpened, DataBoard,
  List, Grid, Histogram, TrendCharts, Timer,
  Connection, OfficeBuilding, House, Shop,
  HomeFilled, Location, MapLocation, Edit, Delete,
  Plus, Search, Download, Upload, Bell,
  Calendar, ChatDotRound, Management,
  Monitor, DataAnalysis, Opportunity, Avatar, Tools
} from '@element-plus/icons-vue'

const biIconMap = {
  'bi-speedometer2': Odometer,
  'bi-compass': Location,
  'bi-people': User,
  'bi-person': User,
  'bi-person-circle': Avatar,
  'bi-key': Key,
  'bi-lock': Lock,
  'bi-shield-lock': Lock,
  'bi-gear': Setting,
  'bi-sliders': Tools,
  'bi-folder': Folder,
  'bi-folder-open': FolderOpened,
  'bi-file-earmark': Document,
  'bi-file-text': Document,
  'bi-list': List,
  'bi-grid': Grid,
  'bi-house': HomeFilled,
  'bi-building': OfficeBuilding,
  'bi-shop': Shop,
  'bi-cart': Shop,
  'bi-geo-alt': MapLocation,
  'bi-bar-chart': Histogram,
  'bi-bar-chart-line': TrendCharts,
  'bi-graph-up': TrendCharts,
  'bi-activity': DataAnalysis,
  'bi-clock-history': Timer,
  'bi-bell': Bell,
  'bi-calendar': Calendar,
  'bi-chat-dots': ChatDotRound,
  'bi-clipboard-data': DataBoard,
  'bi-diagram-3': Management,
  'bi-display': Monitor,
  'bi-lightning': Opportunity,
  'bi-puzzle': Grid,
  'bi-tree': List,
  'bi-database': DataAnalysis,
  'bi-hdd': DataBoard,
  'bi-cloud': Connection,
  'bi-globe': MapLocation,
  'bi-envelope': ChatDotRound,
  'bi-tag': List,
  'bi-tags': List,
  'bi-bookmark': List,
  'bi-eye': Search,
  'bi-pencil': Edit,
  'bi-trash': Delete,
  'bi-plus': Plus,
  'bi-search': Search,
  'bi-download': Download,
  'bi-upload': Upload,
  'bi-arrow-repeat': Connection,
  'bi-exclamation-triangle': Opportunity,
  'bi-info-circle': ChatDotRound,
  'bi-question-circle': ChatDotRound,
  'bi-box-arrow-right': Connection,
  'bi-menu-button-wide': MenuIcon,
  'bi-layout-sidebar': MenuIcon,
  'bi-table': Grid,
  'bi-card-list': List
}

const iconColorMap = {
  'primary': '#3b82f6',
  'success': '#10b981',
  'warning': '#f59e0b',
  'danger': '#ef4444',
  'info': '#6b7280',
  'blue': '#3b82f6',
  'green': '#10b981',
  'yellow': '#f59e0b',
  'red': '#ef4444',
  'orange': '#f97316',
  'purple': '#8b5cf6',
  'pink': '#ec4899',
  'cyan': '#06b6d4'
}

export function getMenuIcon(icon) {
  if (!icon) return Document
  return biIconMap[icon] || Document
}

export function getIconColorStyle(iconColor) {
  if (!iconColor) return {}
  const color = iconColorMap[iconColor] || iconColor
  return { color }
}
