/** 报文生成请求：字段全部可缺省，businessId 为空时由后端生成 32 位 UUID。 */
export interface ReportGenerateRequest {
  businessId?: string
  messageType?: string
  payload?: string
}

/** 生成 + 上传结果；uploaded=false 表示后端走了降级（报文已生成但下游上传失败）。 */
export interface ReportUploadResult {
  businessId: string
  messageType: string
  createdAt: string
  xml: string
  uploaded: boolean
  receiptNo: string | null
  storedPath: string | null
  degradeReason: string | null
}

export interface UploadReceiptRequest {
  businessId: string
  messageType: string
  xml: string
}

export interface ReceiptResponse {
  receiptNo: string
  storedPath: string
  receivedAt: string
  success: boolean
  message: string
}

/** 报文回执视图：比 ReceiptResponse 多业务号与报文类型，回执查询与「报文记录」列表共用。 */
export interface ReceiptView extends ReceiptResponse {
  businessId: string
  messageType: string
}

/** 后端异常出口（GlobalExceptionHandler）返回的结构。 */
export interface ApiErrorBody {
  success?: string
  message?: string
}

export interface HealthComponent {
  status: string
  details?: Record<string, unknown>
}

export interface HealthResponse {
  status: string
  components?: Record<string, HealthComponent>
}

export interface RouteAssertion {
  name: string
  args?: Record<string, string>
}

/** 路由定义体：id/uri/predicates 等字段在不同 Spring Cloud Gateway 版本中层级略有差异。 */
export interface GatewayRouteDefinition {
  id?: string
  uri?: string
  order?: number
  predicates?: RouteAssertion[]
  filters?: RouteAssertion[]
}

/** Actuator /gateway/routes 的元素：兼容 route_definition 嵌套与字段平铺两种结构。 */
export interface GatewayRouteRaw extends GatewayRouteDefinition {
  route_id?: string
  route_definition?: GatewayRouteDefinition
}

/** 网关路由表的展示模型。 */
export interface GatewayRouteView {
  id: string
  uri: string
  order: number
  predicates: string[]
}

/** 报文记录列表项：本地留痕与服务端回执统一成这一种形状展示。 */
export interface ReportHistoryItem {
  id: string
  businessId: string
  messageType: string
  createdAt: string
  recordedAt: string
  xml: string
  uploaded: boolean
  receiptNo: string | null
  storedPath: string | null
  degradeReason: string | null
  /** local = 本浏览器生成（含 XML，可重新上传/删除）；server = 服务端回执（只读，XML 按需拉取） */
  source: 'local' | 'server'
}

export interface LoginRequest {
  username: string
  password: string
}

export interface UserInfo {
  username: string
  nickname: string
  roles: string[]
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
}

/** 商品房转移业务状态：受理 → 办结。 */
export type TransferStatus = 'ACCEPTED' | 'COMPLETED'

/** 商品房转移受理入参。 */
export interface TransferCreateRequest {
  sellerName: string
  sellerIdNo: string
  buyerName: string
  buyerIdNo: string
  houseAddress: string
  houseArea: number
  housePrice: number
}

/** 商品房转移业务视图，受理/办结/查询共用。 */
export interface TransferView {
  businessId: string
  status: TransferStatus
  /** 中文状态描述，由后端给出，前端不硬编码翻译 */
  statusLabel: string
  sellerName: string
  sellerIdNo: string
  buyerName: string
  buyerIdNo: string
  houseAddress: string
  houseArea: number
  housePrice: number
  acceptedAt: string
  completedAt: string | null
  /** 办结时投递的 MQ 消息 ID，便于按业务号反查消息轨迹 */
  messageId: string | null
}
