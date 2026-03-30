# Bable-backend

Spring Boot backend for Bable.

## Routes

### `POST /user/create-user`

Send:

```bash
curl -X POST "$BASE_URL/user/create-user" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Rishi",
    "email": "rishi@example.com",
    "password": "strong-password"
  }'
```

Success:

- `201`
- body: `User Created | ID : <userId>`

Common failures:

- `409` with `User Already Exists`
- `500` with `Internal Server Error`

### `POST /user/login-user`

Send:

```bash
curl -i -X POST "$BASE_URL/user/login-user" \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "rishi@example.com",
    "password": "strong-password"
  }'
```

Success:

- `200`
- `Authorization` response header: `Bearer <jwt>`
- body: `<jwt>`

Common failures:

- `404` with `No User Found`
- `401` with `Unauthorized`

### `GET /user/details-user?id=<userId>`

Public route.

Send:

```bash
curl "$BASE_URL/user/details-user?id=<userId>"
```

Success:

- `200`
- JSON body:

```json
{
  "id": "userId",
  "name": "Rishi",
  "email": "rishi@example.com",
  "createdAt": "...",
  "updatedAt": "...",
  "writtenBlogs": [
    { "id": "blogId", "heading": "First Post" }
  ],
  "storedBlogs": [
    { "id": "blogId", "heading": "Saved Post" }
  ]
}
```

Common failures:

- `404` when user is not found

### `POST /user/update-user`

Protected route.

Send:

```bash
curl -X POST "$BASE_URL/user/update-user" \
  -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d '{
    "id": "<userId-from-token>",
    "name": "Updated Name",
    "email": "updated@example.com"
  }'
```

Success:

- `200`
- body: `Entity Updated`

Common failures:

- `401` when auth is missing/invalid
- `403` with `Forbidden | Incorrect User`

### `GET /blog/main-feed`

Public route.

Send:

```bash
curl "$BASE_URL/blog/main-feed"
```

Success:

- `200`
- JSON array:

```json
[
  {
    "id": "blogId",
    "heading": "Post title",
    "img_url": "https://...",
    "author": {
      "id": "userId",
      "name": "Rishi"
    }
  }
]
```

### `GET /blog/blog-content?id=<blogId>`

Public route.

Send:

```bash
curl "$BASE_URL/blog/blog-content?id=<blogId>"
```

Success:

- `200`
- JSON body:

```json
{
  "id": "blogId",
  "heading": "Post title",
  "content": "Full content",
  "upvotes": 0,
  "img_url": "https://...",
  "author": {
    "id": "userId",
    "name": "Rishi"
  },
  "createdAt": "...",
  "updatedAt": "..."
}
```

Common failures:

- `404` when blog is not found

### `POST /blog/create-blog`

Protected route.

Send:

```bash
curl -X POST "$BASE_URL/blog/create-blog" \
  -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d '{
    "heading": "My first post",
    "content": "Full blog content",
    "img_url": "https://example.com/source-image.jpg"
  }'
```

Success:

- `201`
- body: `Blog Created | <blogId>`

Common failures:

- `401` with `Unauthorized`
- `404` with `User Not Found`
- `500` with `Internal Server Error`

### `DELETE /blog/delete-blog?id=<blogId>`

Protected route.

Send:

```bash
curl -X DELETE "$BASE_URL/blog/delete-blog?id=<blogId>" \
  -H "$AUTH"
```

Success:

- `200`
- body: `Blog Deleted | Id <blogId>`

Common failures:

- `404` with `Blog Not Found`
- `401` with `Unauthorized`
- `403` with `Lack of Access`

### `GET /blog/save-blog?id=<blogId>`

Protected route.

Send:

```bash
curl "$BASE_URL/blog/save-blog?id=<blogId>" \
  -H "$AUTH"
```

Success:

- `200`
- body: `Blog Saved | ID <blogId>`

Also possible:

- `200` with `Blog Already Saved | ID <blogId>`
- `404` with `Blog Not Found`
- `404` with `User Not Found`
- `401` with `Unauthenticated`

### `GET /blog/delete-save?id=<blogId>`

Protected route.

Send:

```bash
curl "$BASE_URL/blog/delete-save?id=<blogId>" \
  -H "$AUTH"
```

Success:

- `200`
- body: `Blog Deleted From Save | ID <blogId>`

Common failures:

- `404` with `Blog Not Found`
- `404` with `User Not Found`
- `403` with `Lack of Access`

## Notes

- Public routes are:
  - `/user/create-user`
  - `/user/login-user`
  - `/user/details-user`
  - `/blog/main-feed`
  - `/blog/blog-content`
- Protected routes require `Authorization: Bearer <jwt>`.
- CORS is configured for `http://localhost:5173` and `https://bable.vercel.app`.


## Redis
- Redis cache is integrated for all listing page, each blog's display page and each user's profile page.
- Cache clears and handles itself to avoid stale data


