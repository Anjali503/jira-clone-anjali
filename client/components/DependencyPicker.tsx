"use client";

import React, { useMemo, useState } from "react";
import { Input } from "./ui/input";

const DependencyPicker = ({
  issues,
  selectedIds,
  onChange,
  currentIssueId,
}: any) => {
  const [query, setQuery] = useState("");

  const filteredIssues = useMemo(() => {
    const search = query.toLowerCase();

    return (issues || [])
      .filter((issue: any) => issue.id !== currentIssueId)
      .filter(
        (issue: any) =>
          issue?.title?.toLowerCase().includes(search) ||
          issue?.key?.toLowerCase().includes(search),
      );
  }, [issues, query, currentIssueId]);

  const toggleDependency = (issueId: string) => {
    if (selectedIds.includes(issueId)) {
      onChange(selectedIds.filter((id: string) => id !== issueId));
      return;
    }

    onChange([...selectedIds, issueId]);
  };

  return (
    <div className="space-y-2">
      <Input
        placeholder="Search issues..."
        className="h-9 border-[#DFE1E6] focus-visible:ring-[#0052CC]"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <div className="max-h-36 overflow-y-auto rounded border border-[#DFE1E6] bg-white">
        {filteredIssues.length > 0 ? (
          filteredIssues.map((issue: any) => (
            <label
              key={issue.id}
              className="flex cursor-pointer items-center gap-2 px-3 py-2 text-sm hover:bg-[#F4F5F7]"
            >
              <input
                type="checkbox"
                checked={selectedIds.includes(issue.id)}
                onChange={() => toggleDependency(issue.id)}
              />
              <span className="truncate text-[#172B4D]">
                {issue.key ? `${issue.key} - ` : ""}
                {issue.title}
              </span>
            </label>
          ))
        ) : (
          <p className="px-3 py-2 text-sm text-[#6B778C]">No issues found</p>
        )}
      </div>
    </div>
  );
};

export default DependencyPicker;
