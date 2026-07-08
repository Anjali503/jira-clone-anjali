"use client";

import { Bell, RefreshCcw } from "lucide-react";
import { useEffect, useState } from "react";
import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";
import { Badge } from "./ui/badge";
import { Button } from "./ui/button";

type Notification = {
  id: string;
  message: string;
  type: string;
  read: boolean;
  timestamp: string;
};

const formatTimestamp = (timestamp: string) => {
  try {
    return new Date(timestamp).toLocaleString();
  } catch {
    return timestamp;
  }
};

const NotificationBell = () => {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const loadNotifications = async () => {
    if (!user?.id) return;
    setLoading(true);
    setError("");
    try {
      const res = await axiosInstance.get(`/api/notifications/user/${user.id}`);
      setNotifications(res.data || []);
    } catch (err) {
      console.error("Failed to load notifications", err);
      setError("Unable to load notifications.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, [user?.id]);

  const unreadCount = notifications.filter((notification) => !notification.read).length;

  const toggleNotificationRead = async (notification: Notification) => {
    if (!notification?.id) return;
    try {
      const path = notification.read ? "unread" : "read";
      await axiosInstance.put(`/api/notifications/${notification.id}/${path}`);
      await loadNotifications();
    } catch (err) {
      console.error("Failed to update notification read state", err);
      setError("Unable to update notification.");
    }
  };

  const handleBellClick = () => {
    setIsOpen((prev) => !prev);
    if (!isOpen) {
      loadNotifications();
    }
  };

  return (
    <div className="relative">
      <Button
        variant="ghost"
        size="icon"
        className="relative text-[#42526E] hover:bg-[#EBECF0]"
        onClick={handleBellClick}
        aria-label="Open notifications"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="pointer-events-none absolute -top-1 -right-1 inline-flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-[#0052CC] px-1.5 text-[10px] font-semibold text-white">
            {unreadCount}
          </span>
        )}
      </Button>

      {isOpen && (
        <div className="absolute right-0 z-50 mt-2 w-80 rounded border border-[#DFE1E6] bg-white shadow-xl">
          <div className="flex items-center justify-between border-b border-[#DFE1E6] px-4 py-3">
            <div>
              <p className="text-sm font-semibold text-[#172B4D]">Notifications</p>
              <p className="text-xs text-[#6B778C]">Latest updates for your account.</p>
            </div>
            <Button
              variant="ghost"
              size="icon"
              className="text-[#6B778C] hover:bg-[#F4F5F7]"
              onClick={loadNotifications}
              aria-label="Refresh notifications"
            >
              <RefreshCcw className="h-4 w-4" />
            </Button>
          </div>
          <div className="max-h-96 overflow-y-auto">
            {loading ? (
              <div className="p-4 text-sm text-[#6B778C]">Loading notifications…</div>
            ) : error ? (
              <div className="p-4 text-sm text-red-600">{error}</div>
            ) : notifications.length === 0 ? (
              <div className="p-4 text-sm text-[#6B778C]">No notifications yet.</div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={`border-b border-[#DFE1E6] px-4 py-3 ${notification.read ? "bg-[#F4F5F7]" : "bg-white"}`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-[#172B4D]">{notification.message}</p>
                      <p className="text-xs text-[#6B778C]">{formatTimestamp(notification.timestamp)}</p>
                    </div>
                    <Button
                      variant="ghost"
                      size="xs"
                      className="text-[#0052CC]"
                      onClick={() => toggleNotificationRead(notification)}
                    >
                      {notification.read ? "Mark unread" : "Mark read"}
                    </Button>
                  </div>
                  <div className="mt-2 flex items-center justify-between gap-2">
                    <Badge variant={notification.read ? "outline" : "default"} className="text-[10px] uppercase tracking-[0.08em]">
                      {notification.read ? "Read" : "Unread"}
                    </Badge>
                    <span className="text-[10px] text-[#6B778C]">{notification.type}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;
