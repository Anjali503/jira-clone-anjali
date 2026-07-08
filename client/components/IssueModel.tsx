"use client";

import React, { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "./ui/dialog";
import { Button } from "./ui/button";
import { Plus, Trash2, Edit, Download } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "./ui/avatar";
import { Textarea } from "./ui/textarea";
import { Badge } from "./ui/badge";
import { Input } from "./ui/input";
import { WorkLogModal } from "./WorkLogModal";
import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";

const priorityLabels: Record<string, string> = {
  HIGH: "High",
  MEDIUM: "Medium",
  LOW: "Low",
};

const typeIcons: Record<string, string> = {
  BUG: "Bug",
  TASK: "Task",
  STORY: "Story",
};

const IssueModel = ({ issue, isOpen, onClose, allIssues = [], onIssueCreated }: any) => {
  const { user } = useAuth();

  const [assignee, setAssignee] = useState<any>(null);
  const [commentText, setCommentText] = useState("");
  const [loading, setLoading] = useState(false);
  const [localIssue, setLocalIssue] = useState<any>(null);
  const [subtasks, setSubtasks] = useState<any[]>([]);
  const [subtaskModalOpen, setSubtaskModalOpen] = useState(false);
  const [subtaskLoading, setSubtaskLoading] = useState(false);
  const [subtaskForm, setSubtaskForm] = useState({
    title: "",
    description: "",
    priority: "MEDIUM",
  });

  // WorkLog state
  const [workLogs, setWorkLogs] = useState<any[]>([]);
  const [workLogLoading, setWorkLogLoading] = useState(false);
  const [workLogError, setWorkLogError] = useState<string>("");
  const [workLogModalOpen, setWorkLogModalOpen] = useState(false);
  const [selectedWorkLog, setSelectedWorkLog] = useState<any>(null);
  const [attachments, setAttachments] = useState<any[]>([]);
  const [attachmentLoading, setAttachmentLoading] = useState(false);
  const [attachmentError, setAttachmentError] = useState<string>("");
  const [attachmentUploadLoading, setAttachmentUploadLoading] = useState(false);
  const [attachmentUploadError, setAttachmentUploadError] = useState<string>("");
  // New sprint state for total logged hours
  const [sprint, setSprint] = useState<any>(null);
  const [sprintLoading, setSprintLoading] = useState(false);
  const [sprintError, setSprintError] = useState<string>("");

  const loadIssueDetails = async (issueToLoad: any) => {
    if (!issueToLoad?.id) return;

    try {
      setLoading(true);
      setWorkLogLoading(true);
      setSprint(null);
      setSprintError("");
      setWorkLogError("");

      setLocalIssue(issueToLoad);

      const issueRes = await axiosInstance.get(`/api/issues/${issueToLoad.id}`);
      const latestIssue = issueRes.data;
      setLocalIssue(latestIssue);

      const workLogRes = await axiosInstance.get(
        `/api/worklogs?issueId=${latestIssue.id}`,
      );
      setWorkLogs(
        (workLogRes.data || []).map((log: any) => ({
          ...log,
          id: normalizeId(log.id),
        })),
      );

      const subtasksRes = await axiosInstance.get(
        `/api/issues/parent/${latestIssue.id}`,
      );
      setSubtasks(subtasksRes.data || []);

      await loadAttachments(latestIssue.id);

      if (latestIssue.sprintId) {
        setSprintLoading(true);
        const sprintRes = await axiosInstance.get(
          `/api/sprints/${latestIssue.sprintId}`,
        );
        setSprint(sprintRes.data);
      }
    } catch (err) {
      console.error("Failed to load issue details", err);
      setWorkLogError("Failed to load work logs.");
      setSprintError("Failed to load sprint data.");
    } finally {
      setLoading(false);
      setWorkLogLoading(false);
      setSprintLoading(false);
    }
  };

  useEffect(() => {
    if (!isOpen || !issue?.id) return;
    loadIssueDetails(issue);
  }, [isOpen, issue?.id]);

  useEffect(() => {
    if (!localIssue?.assigneeId) {
      setAssignee(null);
      return;
    }

    const fetchAssignee = async () => {
      try {
        const res = await axiosInstance.get(
          `/api/users/${localIssue.assigneeId}`,
        );
        setAssignee(res.data);
      } catch (err) {
        console.error("Failed to load assignee", err);
      }
    };

    fetchAssignee();
  }, [localIssue?.assigneeId]);

  const dependencyIssues = allIssues.filter((projectIssue: any) =>
    (localIssue?.dependencies || []).includes(projectIssue.id),
  );

  const saveComment = async () => {
    if (!commentText.trim() || !user || !localIssue) return;

    try {
      setLoading(true);

      const updatedComments = [
        ...(localIssue.comments || []),
        commentText, // ONLY STRING
      ];

      await axiosInstance.put(`/api/issues/${localIssue.id}`, {
        title: localIssue.title,
        description: localIssue.description,
        type: localIssue.type,
        priority: localIssue.priority,
        status: localIssue.status,
        projectId: localIssue.projectId,
        reporterId: localIssue.reporterId,
        assigneeId: localIssue.assigneeId,
        sprintId: localIssue.sprintId ?? null,
        order: localIssue.order ?? 0,
        comments: updatedComments,
        dependencies: localIssue.dependencies || [],
        updatedAt: new Date().toISOString(),
      });

      setLocalIssue((prev: any) => ({
        ...prev,
        comments: updatedComments,
      }));

      setCommentText("");
    } catch (err) {
      console.error("Failed to save comment", err);
    } finally {
      setLoading(false);
    }
  };

  const createSubtask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subtaskForm.title.trim() || !localIssue?.id || !user?.id) return;

    try {
      setSubtaskLoading(true);
      const res = await axiosInstance.post("/api/issues", {
        title: subtaskForm.title,
        description: subtaskForm.description,
        priority: subtaskForm.priority,
        parentId: localIssue.id,
        reporterId: user.id,
        status: "TODO",
      });

      const createdSubtask = res.data;
      setSubtasks((prev) => [...prev, createdSubtask]);
      setSubtaskForm({ title: "", description: "", priority: "MEDIUM" });
      setSubtaskModalOpen(false);
      onIssueCreated?.(createdSubtask);
    } catch (err) {
      console.error("Failed to create subtask", err);
    } finally {
      setSubtaskLoading(false);
    }
  };

  // WorkLog handlers
  const openAddWorkLog = () => {
    setSelectedWorkLog(null);
    setWorkLogModalOpen(true);
  };

  const openEditWorkLog = (log: any) => {
    setSelectedWorkLog(log);
    setWorkLogModalOpen(true);
  };

  const deleteWorkLog = async (logId: any) => {
    const normalizedLogId = normalizeId(logId);
    if (!normalizedLogId) return;
    if (!window.confirm("Delete this work log?")) return;
    try {
      setWorkLogLoading(true);
      await axiosInstance.delete(`/api/worklogs/${normalizedLogId}?confirm=true`, {
        headers: {
          "X-User-Id": user?.id,
        },
      });
      setWorkLogs((prev) => prev.filter((l) => normalizeId(l.id) !== normalizedLogId));
    } catch (err) {
      console.error("Failed to delete work log", err);
      setWorkLogError("Unable to delete work log.");
    } finally {
      setWorkLogLoading(false);
    }
  };

  const normalizeId = (id: any): string | null => {
    if (id == null) return null;
    if (typeof id === "string") return id;
    if (typeof id === "number") return String(id);
    if (typeof id === "object") {
      if (typeof id.$oid === "string") return id.$oid;
      if (typeof id.oid === "string") return id.oid;
      if (typeof id.id === "string") return id.id;
      if (typeof id._id === "string") return id._id;
      return JSON.stringify(id);
    }
    return String(id);
  };

  const getApiErrorMessage = (error: any) => {
    const data = error?.response?.data;
    if (typeof data === "string") return data;
    if (data?.message && typeof data.message === "string") return data.message;
    if (data?.error && typeof data.error === "string") return data.error;
    return error?.message || "An unexpected error occurred.";
  };

  const loadAttachments = async (issueId: string) => {
    if (!issueId || !user?.id) return;
    try {
      setAttachmentLoading(true);
      setAttachmentError("");
      const res = await axiosInstance.get(`/api/attachments/issue/${issueId}`, {
        headers: {
          "X-User-Id": user.id,
        },
      });
      setAttachments(res.data || []);
    } catch (err: any) {
      console.error("Failed to load attachments", err);
      setAttachmentError(getApiErrorMessage(err));
    } finally {
      setAttachmentLoading(false);
    }
  };

  const handleUploadAttachment = async (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file || !localIssue?.id || !user?.id) return;
    try {
      setAttachmentUploadLoading(true);
      setAttachmentUploadError("");

      const formData = new FormData();
      formData.append("file", file);

      await axiosInstance.post(
        `/api/attachments/issue/${localIssue.id}`,
        formData,
        {
          headers: {
            "X-User-Id": user.id,
          },
        },
      );

      await loadAttachments(localIssue.id);
      event.target.value = "";
    } catch (err: any) {
      console.error("Attachment upload failed", err);
      setAttachmentUploadError(getApiErrorMessage(err));
    } finally {
      setAttachmentUploadLoading(false);
    }
  };

  const handleDownloadAttachment = async (attachment: any) => {
    if (!attachment?.id || !user?.id) return;
    try {
      setAttachmentLoading(true);
      setAttachmentError("");
      const response = await axiosInstance.get(
        `/api/attachments/${attachment.id}/download`,
        {
          headers: {
            "X-User-Id": user.id,
          },
          responseType: "blob",
        },
      );
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement("a");
      link.href = url;
      link.download = attachment.originalName || "attachment";
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error("Attachment download failed", err);
      setAttachmentError(getApiErrorMessage(err));
    } finally {
      setAttachmentLoading(false);
    }
  };

  const handleDeleteAttachment = async (attachmentId: string) => {
    if (!attachmentId || !user?.id || !window.confirm("Delete this attachment?"))
      return;
    try {
      setAttachmentLoading(true);
      setAttachmentError("");
      await axiosInstance.delete(`/api/attachments/${attachmentId}`, {
        headers: {
          "X-User-Id": user.id,
        },
      });
      setAttachments((prev) => prev.filter((attachment) => attachment.id !== attachmentId));
    } catch (err: any) {
      console.error("Failed to delete attachment", err);
      setAttachmentError(getApiErrorMessage(err));
    } finally {
      setAttachmentLoading(false);
    }
  };

  const handleWorkLogSaved = async () => {
    // Refresh issue details, including totals, after add/edit
    if (!localIssue?.id) return;

    await loadIssueDetails(localIssue);
  };

  return (
    <>
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto p-0 gap-0 border-none shadow-2xl">
        <DialogHeader className="p-4 border-b flex justify-between items-center">
          <div>
            <DialogTitle className="text-sm font-semibold text-[#5E6C84]">
              {localIssue?.title ?? "Loading issue..."}
            </DialogTitle>
            <DialogDescription className="text-xs text-[#6B778C]">
              View issue details, work logs, comments, and related sprint totals.
            </DialogDescription>
          </div>
          <div className="flex gap-2">
            <Button size="sm" variant="outline" onClick={() => setSubtaskModalOpen(true)}>
              <Plus className="h-4 w-4 mr-1" />
              Create Subtask
            </Button>
            <Button size="sm" variant="outline" onClick={openAddWorkLog} className="bg-[#0052CC] text-white">
              Log Work
            </Button>
          </div>
        </DialogHeader>

        {!localIssue || loading ? (
          <div className="flex h-64 items-center justify-center text-sm text-[#6B778C]">
            Loading issue...
          </div>
        ) : (
          <div className="flex flex-col md:flex-row">
            {/* Main */}
            <div className="flex-1 p-6">
              <h2 className="text-2xl font-semibold mb-4">
                {localIssue.title}
              </h2>

              <h3 className="text-sm font-semibold mb-2">Description</h3>
              <p className="text-sm text-[#42526E] mb-8">
                {localIssue.description || "No description"}
              </p>

              {subtasks.length > 0 && (
                <div className="mb-8 rounded border border-[#DFE1E6] p-4">
                  <h3 className="text-sm font-semibold mb-3">Subtasks</h3>
                  <div className="space-y-2">
                    {subtasks.map((subtask: any) => (
                      <button
                        key={subtask.id}
                        type="button"
                        className="flex w-full items-center gap-2 rounded px-2 py-1 text-left text-sm hover:bg-[#F4F5F7]"
                        onClick={() => loadIssueDetails(subtask)}
                      >
                        <span className="text-[#0052CC]">
                          {subtask.status === "DONE" ? "[x]" : "[ ]"}
                        </span>
                        <span className="truncate text-[#172B4D]">{subtask.title}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <h3 className="text-sm font-semibold mb-4">
                Comments ({localIssue.comments?.length || 0})
              </h3>

              <div className="space-y-4 mb-6">
                {localIssue.comments?.length > 0 ? (
                  <div className="space-y-3">
                    {localIssue.comments.map(
                      (comment: string, index: number) => (
                        <div
                          key={index}
                          className="rounded-md border border-[#DFE1E6] bg-[#F4F5F7] p-3"
                        >
                          <p className="text-sm text-[#172B4D] whitespace-pre-wrap">
                            {comment}
                          </p>
                        </div>
                      ),
                    )}
                  </div>
                ) : (
                  <p className="text-sm text-[#6B778C] italic">
                    No comments yet
                  </p>
                )}
              </div>

              {/* Add Comment */}
              <div className="flex gap-3">
                <Avatar className="h-8 w-8">
                  <AvatarImage src={user?.avatar} />
                  <AvatarFallback>ME</AvatarFallback>
                </Avatar>
                <div className="flex-1">
                  <Textarea
                    placeholder="Add a comment..."
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                  />
                  <div className="flex justify-end mt-2">
                    <Button
                      size="sm"
                      className="bg-[#0052CC] text-white"
                      disabled={!commentText || loading}
                      onClick={saveComment}
                    >
                      Save
                    </Button>
                  </div>
                </div>
              </div>
            </div>

            {/* Sidebar */}
            <div className="w-full md:w-[280px] p-6 border-l">
              <div className="space-y-5">
                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Status</h3>
                  <Badge>{localIssue.status}</Badge>
                </div>

                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Depends On</h3>
                  {dependencyIssues.length > 0 ? (
                    <ul className="space-y-1 text-sm text-[#42526E]">
                      {dependencyIssues.map((dependency: any) => (
                        <li key={dependency.id}>- {dependency.title}</li>
                      ))}
                    </ul>
                  ) : (
                    <span className="text-sm italic text-[#6B778C]">None</span>
                  )}
                </div>

                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Total Logged Hours (Task)</h3>
                  <span className="text-sm font-medium">{localIssue?.totalLoggedHours?.toFixed(2) ?? "0"} hrs</span>
                  {sprint && (
                    <>
                      <h3 className="text-xs font-bold uppercase mt-4 mb-1">Total Logged Hours (Sprint)</h3>
                      <span className="text-sm font-medium">{sprint?.totalLoggedHours?.toFixed(2) ?? "0"} hrs</span>
                    </>
                  )}
                  <div className="mt-4"></div>
                </div>
                {/* Work Logs */}
                <div className="mt-4">
                  <h3 className="text-xs font-bold uppercase mb-1">Work Logs</h3>
                  {workLogLoading ? (
                    <p className="text-sm text-[#6B778C]">Loading work logs...</p>
                  ) : workLogs.length > 0 ? (
                    <ul className="space-y-2">
                      {workLogs.map((log, index) => {
                        const key = log.id
                          ? typeof log.id === "string"
                            ? log.id
                            : JSON.stringify(log.id)
                          : `${log.date}-${log.duration}-${index}`;
                        return (
                          <li key={key} className="flex justify-between items-center border p-2 rounded">
                            <div>
                              <p className="text-sm font-medium">{new Date(log.date).toLocaleDateString()}</p>
                              <p className="text-xs text-[#5E6C84]">{log.duration} hrs - {log.description}</p>
                            </div>
                            <div className="flex gap-2">
                              <button onClick={() => openEditWorkLog(log)} title="Edit">
                                <Edit className="h-4 w-4 text-[#0052CC]" />
                              </button>
                              <button onClick={() => deleteWorkLog(log.id)} title="Delete">
                                <Trash2 className="h-4 w-4 text-red-600" />
                              </button>
                            </div>
                          </li>
                        );
                      })}
                    </ul>
                  ) : (
                    <p className="text-sm text-[#6B778C] italic">No work logs yet.</p>
                  )}
                  {workLogError && (
                    <p className="text-xs text-red-600 mt-1">{workLogError}</p>
                  )}
                </div>

                <div className="mt-4">
                  <div className="flex items-center justify-between mb-2">
                    <div>
                      <h3 className="text-xs font-bold uppercase">Attachments</h3>
                      <p className="text-xs text-[#6B778C]">Upload and manage files for this issue.</p>
                    </div>
                    <label className="inline-flex cursor-pointer items-center">
                      <input
                        type="file"
                        className="hidden"
                        accept=".pdf,.png,.jpg,.jpeg,.docx"
                        onChange={handleUploadAttachment}
                        disabled={attachmentUploadLoading}
                      />
                      <Button size="sm" className="bg-[#0052CC] text-white" disabled={attachmentUploadLoading}>
                        {attachmentUploadLoading ? "Uploading…" : "Upload"}
                      </Button>
                    </label>
                  </div>

                  {attachmentLoading ? (
                    <p className="text-sm text-[#6B778C]">Loading attachments...</p>
                  ) : attachments.length > 0 ? (
                    <ul className="space-y-2">
                      {attachments.map((attachment) => (
                        <li key={attachment.id} className="rounded border border-[#DFE1E6] p-2">
                          <div className="flex items-center justify-between gap-2">
                            <div>
                              <p className="text-sm font-medium text-[#172B4D]">{attachment.originalName}</p>
                              <p className="text-xs text-[#5E6C84]">{(attachment.fileSize / 1024).toFixed(1)} KB</p>
                            </div>
                            <div className="flex items-center gap-2">
                              <button
                                type="button"
                                onClick={() => handleDownloadAttachment(attachment)}
                                title="Download"
                                className="text-[#0052CC] hover:text-[#0747A6]"
                              >
                                <Download className="h-4 w-4" />
                              </button>
                              <button
                                type="button"
                                onClick={() => handleDeleteAttachment(attachment.id)}
                                title="Delete"
                                className="text-red-600 hover:text-red-700"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          </div>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-sm text-[#6B778C] italic">No attachments yet.</p>
                  )}
                  {(attachmentError || attachmentUploadError) && (
                    <p className="text-xs text-red-600 mt-2">
                      {attachmentUploadError || attachmentError}
                    </p>
                  )}
                </div>

                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Type</h3>
                  <span>
                    {typeIcons[localIssue.type]} {localIssue.type}
                  </span>
                </div>

                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Priority</h3>
                  <Badge>{priorityLabels[localIssue.priority]}</Badge>
                </div>

                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Assignee</h3>
                  {assignee ? (
                    <div className="flex items-center gap-2">
                      <Avatar className="h-6 w-6">
                        <AvatarImage src={assignee.avatar} />
                        <AvatarFallback>{assignee.name[0]}</AvatarFallback>
                      </Avatar>
                      <span className="text-sm">{assignee.name}</span>
                    </div>
                  ) : (
                    <span className="text-sm italic">Unassigned</span>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
    <Dialog open={subtaskModalOpen} onOpenChange={setSubtaskModalOpen}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Create Subtask</DialogTitle>
          <DialogDescription className="text-xs text-[#6B778C]">
            Add a new subtask under the current issue with title, description, and priority.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={createSubtask} className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-semibold text-[#172B4D]">
              Title *
            </label>
            <Input
              required
              value={subtaskForm.title}
              onChange={(e) =>
                setSubtaskForm((prev) => ({ ...prev, title: e.target.value }))
              }
              className="h-10 border-[#DFE1E6] focus-visible:ring-[#0052CC]"
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-semibold text-[#172B4D]">
              Description
            </label>
            <Textarea
              value={subtaskForm.description}
              onChange={(e) =>
                setSubtaskForm((prev) => ({
                  ...prev,
                  description: e.target.value,
                }))
              }
              className="min-h-[100px] border-[#DFE1E6] focus-visible:ring-[#0052CC] resize-none"
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-semibold text-[#172B4D]">
              Priority
            </label>
            <select
              value={subtaskForm.priority}
              onChange={(e) =>
                setSubtaskForm((prev) => ({
                  ...prev,
                  priority: e.target.value,
                }))
              }
              className="w-full h-10 rounded border border-[#DFE1E6] bg-white px-3 text-sm text-[#172B4D] focus-visible:ring-2 focus-visible:ring-[#0052CC]"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </div>
          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button
              type="button"
              variant="outline"
              onClick={() => setSubtaskModalOpen(false)}
              disabled={subtaskLoading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="bg-[#0052CC] text-white hover:bg-[#0747A6]"
              disabled={subtaskLoading}
            >
              {subtaskLoading ? "Creating..." : "Create Subtask"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
    <WorkLogModal
        taskId={localIssue?.id}
        workLog={selectedWorkLog}
        open={workLogModalOpen}
        onClose={() => setWorkLogModalOpen(false)}
        onSaved={handleWorkLogSaved}
      />
    </>
  );
};

export default IssueModel;

