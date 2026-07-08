/* src/client/components/WorkLogModal.tsx */
"use client";

import React, { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "./ui/dialog";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Textarea } from "./ui/textarea";
import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";

export interface WorkLog {
  id: string;
  date: string; // ISO string
  duration: number;
  description: string;
  taskId: string;
}

interface WorkLogModalProps {
  taskId: string;
  workLog?: WorkLog; // undefined for create
  open: boolean;
  onClose: () => void;
  onSaved: () => void; // refresh parent list after save
}

export const WorkLogModal: React.FC<WorkLogModalProps> = ({
  taskId,
  workLog,
  open,
  onClose,
  onSaved,
}) => {
  const { user } = useAuth();
  const isEdit = !!workLog;

  const [date, setDate] = useState("");
  const [duration, setDuration] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Sync state when workLog change
  useEffect(() => {
    if (open) {
      setDate(workLog?.date?.slice(0, 10) ?? new Date().toISOString().slice(0, 10));
      setDuration(workLog?.duration?.toString() ?? "");
      setDescription(workLog?.description ?? "");
      setError("");
    }
  }, [open, workLog]);

  const handleSave = async () => {
    if (!date || !duration) {
      setError("Date and duration are required.");
      return;
    }
    
    if (isEdit) {
      const confirmEdit = window.confirm("Are you sure you want to update this work log?");
      if (!confirmEdit) return;
    }

    setLoading(true);
    setError("");

    try {
      const payload = {
        date: new Date(date).toISOString(),
        duration: parseFloat(duration),
        description,
        taskId,
      };

      const headers = {
        "X-User-Id": user?.id || "",
      };

      if (isEdit) {
        await axiosInstance.put(`/api/worklogs/${workLog!.id}?confirm=true`, payload, { headers });
      } else {
        await axiosInstance.post("/api/worklogs", payload, { headers });
      }

      onSaved();
      onClose();
    } catch (e: any) {
      console.error(e);
      const backendError = e.response?.data?.message || e.response?.data || "Failed to save work log.";
      setError(backendError);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-md bg-white">
        <DialogHeader>
          <DialogTitle className="text-lg font-semibold text-[#172B4D]">
            {isEdit ? "Edit Work Log" : "Add Work Log"}
          </DialogTitle>
          <DialogDescription className="text-sm text-[#5E6C84]">
            Enter the date, duration in hours, and a short note for this work log.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4 py-4">
          {error && (
            <div className="rounded bg-red-50 p-2.5 text-xs font-medium text-red-600 border border-red-200">
              {error}
            </div>
          )}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-[#5E6C84]">Date *</label>
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-semibold text-[#5E6C84]">Duration (hours) *</label>
            <Input
              type="number"
              min={0}
              step={0.1}
              placeholder="e.g., 2.5"
              value={duration}
              onChange={(e) => setDuration(e.target.value)}
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs font-semibold text-[#5E6C84]">Description</label>
            <Textarea
              placeholder="What did you work on?"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t">
            <Button variant="outline" onClick={onClose} disabled={loading}>
              Cancel
            </Button>
            <Button disabled={loading} onClick={handleSave} className="bg-[#0052CC] text-white hover:bg-[#0747A6]">
              {loading ? "Saving…" : "Save"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};
