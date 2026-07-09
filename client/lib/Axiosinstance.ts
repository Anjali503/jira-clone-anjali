import axios from "axios";

const axiosInstance = axios.create({
  baseURL:
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    "https://jira-clone-backend-rpa6.onrender.com",
});

export default axiosInstance;