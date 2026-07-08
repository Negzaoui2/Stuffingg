# syntax=docker/dockerfile:1.7

# --- Build Angular ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Runtime Nginx ---
FROM nginx:alpine AS runtime
# ⚠️ Adapte le nom du dossier de sortie (voir note ci-dessous)
COPY --from=build /app/dist/ /usr/share/nginx/html/
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
