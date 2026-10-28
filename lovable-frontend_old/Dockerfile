# Stage 1: Build the React App
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ENV VITE_API_URL=http://api.ideastitch.in
RUN npm run build

# Stage 2: Serve with NGINX
FROM nginx:alpine
# Copy the built Vite assets to NGINX's serving directory
COPY --from=build /app/dist /usr/share/nginx/html

# Add a custom NGINX config to handle React Router (SPA routing)
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]