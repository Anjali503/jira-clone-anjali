"use client";

import React, { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "./ui/dialog";
import { Button } from "./ui/button";
import { MoreHorizontal, Trash2, ExternalLink, Clock, Edit } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "./ui/avatar";
import { Textarea } from "./ui/textarea";
import { Badge } from "./ui/badge";
import { WorkLogModal } from "./WorkLogModal";
import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";

const priorityLabels: Record<string, string> = {
  HIGH: "High",
  MEDIUM: "Medium",
  LOW: "Low",
};

const typeIcons: Record<string, string> = {
  BUG: "🐛",
  TASK: "✓",
  STORY: "📖",
};

const IssueModel = ({ issue, isOpen, onClose }: any) => {
  const { user } = useAuth();

  const [assignee, setAssignee] = useState<any>(null);
  const [commentText, setCommentText] = useState("");
  const [loading, setLoading] = useState(false);
  const [localIssue, setLocalIssue] = useState<any>(null);

  // WorkLog state
  const [workLogs, setWorkLogs] = useState<any[]>([]);
  const [workLogLoading, setWorkLogLoading] = useState(false);
  const [workLogError, setWorkLogError] = useState<string>("");
  const [workLogModalOpen, setWorkLogModalOpen] = useState(false);
  const [selectedWorkLog, setSelectedWorkLog] = useState<any>(null);
  // New sprint state for total logged hours
  const [sprint, setSprint] = useState<any>(null);
  const [sprintLoading, setSprintLoading] = useState(false);
  const [sprintError, setSprintError] = useState<string>("");

  useEffect(() => {
    if (!isOpen || !issue?.id) return;

    const fetchIssue = async () => {
      try {
        setLoading(true);
        setLocalIssue(issue);
        // Fetch work logs for this issue
        setWorkLogLoading(true);
        const res = await axiosInstance.get(`/api/worklogs?issueId=${issue.id}`);
        setWorkLogs(res.data);
        // If issue has sprint, fetch sprint details for total logged hours
        if (issue.sprintId) {
          setSprintLoading(true);
          const sprintRes = await axiosInstance.get(`/api/sprints/${issue.sprintId}`);
          setSprint(sprintRes.data);
        }
      } catch (err) {
        console.error("Failed to load issue or worklogs", err);
        setWorkLogError("Failed to load work logs.");
        setSprintError("Failed to load sprint data.");
      } finally {
        setLoading(false);
        setWorkLogLoading(false);
        setSprintLoading(false);
      }
    };

    fetchIssue();
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

  // WorkLog handlers
  const openAddWorkLog = () => {
    setSelectedWorkLog(null);
    setWorkLogModalOpen(true);
  };

  const openEditWorkLog = (log: any) => {
    setSelectedWorkLog(log);
    setWorkLogModalOpen(true);
  };

  const deleteWorkLog = async (logId: string) => {
    if (!window.confirm("Delete this work log?")) return;
    try {
      setWorkLogLoading(true);
      await axiosInstance.delete(`/api/worklogs/${logId}?confirm=true`);
      setWorkLogs((prev) => prev.filter((l) => l.id !== logId));
    } catch (err) {
      console.error("Failed to delete work log", err);
      setWorkLogError("Unable to delete work log.");
    } finally {
      setWorkLogLoading(false);
    }
  };

  const handleWorkLogSaved = async () => {
    // Refresh worklogs after add/edit
    try {
      setWorkLogLoading(true);
      const res = await axiosInstance.get(`/api/worklogs?issueId=${localIssue?.id}`);
      setWorkLogs(res.data);
    } catch (err) {
      console.error("Failed to refresh work logs", err);
      setWorkLogError("Failed to refresh work logs.");
    } finally {
      setWorkLogLoading(false);
    }
  };

  return (
    <>
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto p-0 gap-0 border-none shadow-2xl">
        <DialogHeader className="p-4 border-b flex justify-between items-center">
          <DialogTitle className="text-sm font-semibold text-[#5E6C84]">
            {localIssue?.title ?? "Loading issue…"}
          </DialogTitle>
          <Button size="sm" variant="outline" onClick={openAddWorkLog} className="bg-[#0052CC] text-white">
            Log Work
          </Button>
        </DialogHeader>

        {!localIssue || loading ? (
          <div className="flex h-64 items-center justify-center text-sm text-[#6B778C]">
            Loading issue…
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
                      {workLogs.map((log) => (
                        <li key={log.id} className="flex justify-between items-center border p-2 rounded">
                          <div>
                            <p className="text-sm font-medium">{new Date(log.date).toLocaleDateString()}</p>
                            <p className="text-xs text-[#5E6C84]">{log.duration} hrs – {log.description}</p>
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
                      ))}
                    </ul>
                  ) : (
                    <p className="text-sm text-[#6B778C] italic">No work logs yet.</p>
                  )}
                  {workLogError && (
                    <p className="text-xs text-red-600 mt-1">{workLogError}</p>
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
