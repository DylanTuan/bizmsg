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

/** 本地保存的报文生成记录：后端暂无历史查询接口，先落 localStorage 便于回溯。 */
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
