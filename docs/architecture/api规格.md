# API规格

## REST API规格

```yaml
openapi: 3.0.0
info:
  title: 成都酒店综合管理系统 API
  version: 1.0.0
  description: 为成都酒店管理系统提供的RESTful API接口
servers:
  - url: https://api.chengdu-hotel.com/v1
    description: 生产环境
  - url: https://staging-api.chengdu-hotel.com/v1
    description: 测试环境
  - url: http://localhost:8080/v1
    description: 开发环境

paths:
  # 用户认证相关
  /auth/register:
    post:
      tags: [认证]
      summary: 用户注册
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUserRequest'
      responses:
        '201':
          description: 注册成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuthResponse'
        '400':
          $ref: '#/components/responses/BadRequest'

  /auth/login:
    post:
      tags: [认证]
      summary: 用户登录
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginRequest'
      responses:
        '200':
          description: 登录成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuthResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'

  /auth/logout:
    post:
      tags: [认证]
      summary: 用户登出
      security:
        - bearerAuth: []
      responses:
        '200':
          description: 登出成功

  # 用户管理
  /users/me:
    get:
      tags: [用户]
      summary: 获取当前用户信息
      security:
        - bearerAuth: []
      responses:
        '200':
          description: 用户信息
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
    put:
      tags: [用户]
      summary: 更新当前用户信息
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateUserRequest'
      responses:
        '200':
          description: 更新成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'

  # 酒店管理
  /hotels:
    get:
      tags: [酒店]
      summary: 获取酒店列表
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 酒店列表
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HotelListResponse'
    post:
      tags: [酒店]
      summary: 创建酒店
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateHotelRequest'
      responses:
        '201':
          description: 创建成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Hotel'

  /hotels/{id}:
    get:
      tags: [酒店]
      summary: 获取酒店详情
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: 酒店详情
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Hotel'
    put:
      tags: [酒店]
      summary: 更新酒店信息
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateHotelRequest'
      responses:
        '200':
          description: 更新成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Hotel'

  # 房间管理
  /hotels/{hotelId}/rooms:
    get:
      tags: [房间]
      summary: 获取酒店房间列表
      parameters:
        - name: hotelId
          in: path
          required: true
          schema:
            type: integer
        - name: roomTypeId
          in: query
          schema:
            type: integer
        - name: status
          in: query
          schema:
            type: string
            enum: [AVAILABLE, OCCUPIED, MAINTENANCE, CLEANING]
      responses:
        '200':
          description: 房间列表
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RoomListResponse'

  /rooms/{id}:
    get:
      tags: [房间]
      summary: 获取房间详情
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: 房间详情
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Room'

  # 房间搜索
  /rooms/search:
    post:
      tags: [房间]
      summary: 搜索可用房间
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RoomSearchRequest'
      responses:
        '200':
          description: 搜索结果
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RoomSearchResponse'

  # 订单管理
  /orders:
    get:
      tags: [订单]
      summary: 获取用户订单列表
      security:
        - bearerAuth: []
      parameters:
        - name: status
          in: query
          schema:
            type: string
            enum: [PENDING, CONFIRMED, CANCELLED, COMPLETED]
      responses:
        '200':
          description: 订单列表
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OrderListResponse'
    post:
      tags: [订单]
      summary: 创建订单
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        '201':
          description: 创建成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Order'

  /orders/{id}:
    get:
      tags: [订单]
      summary: 获取订单详情
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: 订单详情
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Order'
    put:
      tags: [订单]
      summary: 更新订单
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateOrderRequest'
      responses:
        '200':
          description: 更新成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Order'
    delete:
      tags: [订单]
      summary: 取消订单
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: 取消成功

  # 评价管理
  /reviews:
    get:
      tags: [评价]
      summary: 获取评价列表
      parameters:
        - name: hotelId
          in: query
          schema:
            type: integer
        - name: roomId
          in: query
          schema:
            type: integer
        - name: rating
          in: query
          schema:
            type: integer
            minimum: 1
            maximum: 5
      responses:
        '200':
          description: 评价列表
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReviewListResponse'
    post:
      tags: [评价]
      summary: 提交评价
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateReviewRequest'
      responses:
        '201':
          description: 提交成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Review'

  /reviews/{id}:
    get:
      tags: [评价]
      summary: 获取评价详情
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: 评价详情
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Review'

components:
  schemas:
    User:
      type: object
      properties:
        id:
          type: integer
          format: int64
        username:
          type: string
        email:
          type: string
          format: email
        phone:
          type: string
        role:
          type: string
          enum: [USER, ADMIN]
        status:
          type: string
          enum: [ACTIVE, INACTIVE]
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    CreateUserRequest:
      type: object
      required: [username, email, phone, password]
      properties:
        username:
          type: string
          minLength: 3
          maxLength: 50
        email:
          type: string
          format: email
        phone:
          type: string
          pattern: '^1[3-9]\d{9}$'
        password:
          type: string
          minLength: 8
        role:
          type: string
          enum: [USER, ADMIN]
          default: USER

    LoginRequest:
      type: object
      required: [login, password]
      properties:
        login:
          type: string
          description: 用户名、邮箱或手机号
        password:
          type: string

    AuthResponse:
      type: object
      properties:
        token:
          type: string
        user:
          $ref: '#/components/schemas/User'
        expiresIn:
          type: integer
          description: 令牌过期时间（秒）

    Hotel:
      type: object
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        address:
          type: string
        phone:
          type: string
        description:
          type: string
        facilities:
          type: array
          items:
            type: string
        images:
          type: array
          items:
            type: string
        status:
          type: string
          enum: [ACTIVE, INACTIVE]
        createdBy:
          type: integer
          format: int64
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    Room:
      type: object
      properties:
        id:
          type: integer
          format: int64
        hotelId:
          type: integer
          format: int64
        roomTypeId:
          type: integer
          format: int64
        roomNumber:
          type: string
        floor:
          type: integer
        area:
          type: integer
        status:
          type: string
          enum: [AVAILABLE, OCCUPIED, MAINTENANCE, CLEANING]
        price:
          type: number
          format: decimal
        images:
          type: array
          items:
            type: string
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    Order:
      type: object
      properties:
        id:
          type: integer
          format: int64
        orderNumber:
          type: string
        userId:
          type: integer
          format: int64
        roomId:
          type: integer
          format: int64
        checkInDate:
          type: string
          format: date
        checkOutDate:
          type: string
          format: date
        guestCount:
          type: integer
          minimum: 1
          maximum: 10
        totalPrice:
          type: number
          format: decimal
        status:
          type: string
          enum: [PENDING, CONFIRMED, CANCELLED, COMPLETED]
        specialRequests:
          type: string
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    Review:
      type: object
      properties:
        id:
          type: integer
          format: int64
        userId:
          type: integer
          format: int64
        orderId:
          type: integer
          format: int64
        roomId:
          type: integer
          format: int64
        hotelId:
          type: integer
          format: int64
        overallRating:
          type: integer
          minimum: 1
          maximum: 5
        cleanlinessRating:
          type: integer
          minimum: 1
          maximum: 5
        serviceRating:
          type: integer
          minimum: 1
          maximum: 5
        facilitiesRating:
          type: integer
          minimum: 1
          maximum: 5
        locationRating:
          type: integer
          minimum: 1
          maximum: 5
        comment:
          type: string
          maxLength: 1000
        images:
          type: array
          items:
            type: string
        isAnonymous:
          type: boolean
        status:
          type: string
          enum: [PENDING, APPROVED, REJECTED]
        createdAt:
          type: string
          format: date-time

    RoomSearchRequest:
      type: object
      required: [checkInDate, checkOutDate, guestCount]
      properties:
        hotelId:
          type: integer
          format: int64
        roomTypeId:
          type: integer
          format: int64
        checkInDate:
          type: string
          format: date
        checkOutDate:
          type: string
          format: date
        guestCount:
          type: integer
          minimum: 1
        priceMin:
          type: number
          format: decimal
        priceMax:
          type: number
          format: decimal

    RoomSearchResponse:
      type: object
      properties:
        rooms:
          type: array
          items:
            $ref: '#/components/schemas/Room'
        total:
          type: integer
        page:
          type: integer
        size:
          type: integer

  responses:
    BadRequest:
      description: 请求参数错误
      content:
        application/json:
          schema:
            type: object
            properties:
              error:
                type: object
                properties:
                  code:
                    type: string
                  message:
                    type: string
                  timestamp:
                    type: string
                    format: date-time
                  requestId:
                    type: string

    Unauthorized:
      description: 认证失败
      content:
        application/json:
          schema:
            $ref: '#/components/responses/BadRequest'

  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```
