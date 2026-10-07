# ---- Build stage ----------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# No VITE_API_URL needed in production: the client falls back to same-origin /api,
# which the nginx config reverse-proxies to the backend container.
RUN npm run build

# ---- Runtime stage --------------------------------------------------------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

HEALTHCHECK --interval=20s --timeout=5s --retries=6 \
  CMD wget -q -O /dev/null http://localhost/ || exit 1
