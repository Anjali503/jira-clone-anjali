"use client"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/lib/AuthContext";
import axiosInstance from "@/lib/Axiosinstance";
import { Mail, Save } from "lucide-react";
import React, { useEffect, useRef, useState } from "react";

interface ProfileForm {
  name: string;
  email: string;
  phone: string;
  group: string;
}

interface PasswordForm {
  currentPassword: string;
  newPassword: string;
}

interface EmailForm {
  newEmail: string;
}

interface DeactivateForm {
  currentPassword: string;
}

const page = () => {
  const { user, login } = useAuth();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [profile, setProfile] = useState<ProfileForm | null>(null);
  const [profileForm, setProfileForm] = useState<ProfileForm>({
    name: "",
    email: "",
    phone: "",
    group: "",
  });
  const [passwordForm, setPasswordForm] = useState<PasswordForm>({
    currentPassword: "",
    newPassword: "",
  });
  const [emailForm, setEmailForm] = useState<EmailForm>({ newEmail: "" });
  const [deactivateForm, setDeactivateForm] = useState<DeactivateForm>({
    currentPassword: "",
  });

  const [loadingProfile, setLoadingProfile] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [emailRequesting, setEmailRequesting] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const currentProfile = profile || {
    name: user?.name || "",
    email: user?.email || "",
    phone: user?.phone || "",
    group: user?.group || "",
  };

  useEffect(() => {
    const loadProfile = async () => {
      if (!user?.id) return;
      try {
        setLoadingProfile(true);
        setErrorMessage(null);
        const res = await axiosInstance.get(`/api/users/${user.id}`);
        const data = res.data;
        setProfile({
          name: data.name || "",
          email: data.email || "",
          phone: data.phone || "",
          group: data.group || "",
        });
        setProfileForm({
          name: data.name || "",
          email: data.email || "",
          phone: data.phone || "",
          group: data.group || "",
        });
        if (data) {
          login({
            ...user,
            name: data.name || user.name,
            email: data.email || user.email,
            avatar: data.avatar || user.avatar,
            group: data.group || user.group,
            createdAt: data.createdAt || user.createdAt,
            lastLoginAt: data.lastLoginAt || user.lastLoginAt,
          });
        }
      } catch (error: any) {
        setErrorMessage(
          error.response?.data?.message || "Failed to load profile.",
        );
      } finally {
        setLoadingProfile(false);
      }
    };
    loadProfile();
  }, [user?.id, login, user]);

  const handleProfileChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = e.target;
    setProfileForm((prev) => ({ ...prev, [name]: value }));
    setStatusMessage(null);
    setErrorMessage(null);
  };

  const handleSaveProfile = async () => {
    if (!user?.id) return;
    try {
      setSavingProfile(true);
      setErrorMessage(null);
      setStatusMessage(null);
      const res = await axiosInstance.put(
        `/api/users/${user.id}/profile`,
        {
          name: profileForm.name,
          phone: profileForm.phone,
          group: profileForm.group,
        },
        {
          headers: {
            "X-User-Id": user.id,
          },
        },
      );
      const updated = res.data;
      setProfile({
        name: updated.name || "",
        email: updated.email || profileForm.email,
        phone: updated.phone || "",
        group: updated.group || "",
      });
      setStatusMessage("Profile saved successfully.");
      login({
        ...user,
        name: updated.name || user.name,
        avatar: updated.avatar || user.avatar,
        group: updated.group || user.group,
      });
    } catch (error: any) {
      setErrorMessage(error.response?.data?.message || "Failed to save profile.");
    } finally {
      setSavingProfile(false);
    }
  };

  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleAvatarChange = async (
    e: React.ChangeEvent<HTMLInputElement>,
  ) => {
    if (!user?.id) return;
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      setAvatarUploading(true);
      setErrorMessage(null);
      setStatusMessage(null);
      const formData = new FormData();
      formData.append("file", file);
      const res = await axiosInstance.post(
        `/api/users/${user.id}/avatar`,
        formData,
        {
          headers: {
            "X-User-Id": user.id,
          },
        },
      );
      const updated = res.data;
      login({
        ...user,
        avatar: updated.avatar || user.avatar,
      });
      setStatusMessage("Avatar updated successfully.");
    } catch (error: any) {
      setErrorMessage(error.response?.data?.message || "Failed to upload avatar.");
    } finally {
      setAvatarUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!user?.id) return;
    try {
      setPasswordSaving(true);
      setErrorMessage(null);
      setStatusMessage(null);
      await axiosInstance.put(
        `/api/users/${user.id}/password`,
        {
          currentPassword: passwordForm.currentPassword,
          newPassword: passwordForm.newPassword,
        },
        {
          headers: {
            "X-User-Id": user.id,
          },
        },
      );
      setPasswordForm({ currentPassword: "", newPassword: "" });
      setStatusMessage("Password updated successfully.");
    } catch (error: any) {
      setErrorMessage(error.response?.data?.message || "Failed to update password.");
    } finally {
      setPasswordSaving(false);
    }
  };

  const handleEmailSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!user?.id) return;
    try {
      setEmailRequesting(true);
      setErrorMessage(null);
      setStatusMessage(null);
      await axiosInstance.post(
        `/api/users/${user.id}/email/request-change`,
        {
          newEmail: emailForm.newEmail,
        },
        {
          headers: {
            "X-User-Id": user.id,
          },
        },
      );
      setStatusMessage("Email verification request sent.");
    } catch (error: any) {
      setErrorMessage(error.response?.data?.message || "Failed to request email change.");
    } finally {
      setEmailRequesting(false);
    }
  };

  const handleDeactivateSubmit = async (
    e: React.FormEvent<HTMLFormElement>,
  ) => {
    e.preventDefault();
    if (!user?.id) return;
    if (!confirm("Are you sure you want to deactivate your account?")) {
      return;
    }
    try {
      setDeactivating(true);
      setErrorMessage(null);
      setStatusMessage(null);
      const res = await axiosInstance.put(
        `/api/users/${user.id}/deactivate`,
        {
          currentPassword: deactivateForm.currentPassword,
        },
        {
          headers: {
            "X-User-Id": user.id,
          },
        },
      );
      const updated = res.data;
      login({
        ...user,
        ...updated,
      });
      setStatusMessage("Account deactivated successfully.");
    } catch (error: any) {
      setErrorMessage(error.response?.data?.message || "Failed to deactivate account.");
    } finally {
      setDeactivating(false);
      setDeactivateForm({ currentPassword: "" });
    }
  };

  if (!user) {
    return <div className="p-6">User not found</div>;
  }

  return (
    <div className="flex h-full flex-col p-6 overflow-auto bg-[#F4F5F7]">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-[#172B4D] mb-2">
          Profile Settings
        </h1>
        <p className="text-[#5E6C84]">
          Manage your personal information and preferences
        </p>
      </div>

      {errorMessage && (
        <div className="mb-4 rounded-md bg-red-50 p-4 text-sm text-red-700">
          {errorMessage}
        </div>
      )}
      {statusMessage && (
        <div className="mb-4 rounded-md bg-green-50 p-4 text-sm text-green-700">
          {statusMessage}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle className="text-[#172B4D]">About You</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex flex-col items-center">
                <Avatar className="h-20 w-20 mb-4">
                  <AvatarImage
                    src={user.avatar || "/placeholder.svg"}
                    alt={user.name}
                  />
                  <AvatarFallback>{user.name.charAt(0)}</AvatarFallback>
                </Avatar>
                <h2 className="text-xl font-semibold text-[#172B4D]">
                  {user.name}
                </h2>
                <Badge className="mt-2">{user.role}</Badge>
              </div>

              <div className="space-y-3 pt-4 border-t">
                <div className="flex items-center gap-3 text-sm">
                  <Mail className="h-4 w-4 text-[#5E6C84]" />
                  <span className="text-[#172B4D]">{user.email}</span>
                </div>
                <div className="flex items-center gap-3 text-sm">
                  <span className="text-[#5E6C84]">Group:</span>
                  <Badge variant="outline">{user.group}</Badge>
                </div>
              </div>

              <div className="space-y-3">
                <Button
                  type="button"
                  onClick={handleAvatarClick}
                  disabled={avatarUploading}
                  className="w-full bg-[#0052CC] text-white hover:bg-[#0747A6]"
                >
                  {avatarUploading ? "Uploading…" : "Edit Profile Picture"}
                </Button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/png,image/jpg,image/jpeg"
                  className="hidden"
                  onChange={handleAvatarChange}
                />
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-[#172B4D]">
                Personal Information
              </CardTitle>
              <CardDescription>Update your contact details</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                    Full Name
                  </label>
                  <Input
                    name="name"
                    value={profileForm.name}
                    onChange={handleProfileChange}
                    className="focus-visible:ring-[#0052CC]"
                  />
                </div>
                <div>
                  <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                    Email
                  </label>
                  <Input
                    type="email"
                    name="email"
                    value={profileForm.email}
                    disabled
                    className="focus-visible:ring-[#0052CC]"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                      Role
                    </label>
                    <Input
                      disabled
                      value={user.role}
                      className="focus-visible:ring-[#0052CC]"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                      Team
                    </label>
                    <Input
                      name="group"
                      value={profileForm.group}
                      onChange={handleProfileChange}
                      className="focus-visible:ring-[#0052CC]"
                    />
                  </div>
                </div>
                <div>
                  <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                    Phone
                  </label>
                  <Input
                    name="phone"
                    value={profileForm.phone}
                    onChange={handleProfileChange}
                    className="focus-visible:ring-[#0052CC]"
                  />
                </div>
                <div className="flex justify-end pt-4">
                  <Button
                    type="button"
                    onClick={handleSaveProfile}
                    disabled={savingProfile}
                    className="bg-[#0052CC] text-white hover:bg-[#0747A6]"
                  >
                    <Save className="h-4 w-4 mr-2" />
                    {savingProfile ? "Saving…" : "Save Changes"}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-[#172B4D]">Security</CardTitle>
              <CardDescription>Update your password</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handlePasswordSubmit} className="space-y-4">
                <div>
                  <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                    Current Password
                  </label>
                  <Input
                    type="password"
                    name="currentPassword"
                    value={passwordForm.currentPassword}
                    onChange={(e) =>
                      setPasswordForm((prev) => ({
                        ...prev,
                        currentPassword: e.target.value,
                      }))
                    }
                    className="focus-visible:ring-[#0052CC]"
                  />
                </div>
                <div>
                  <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                    New Password
                  </label>
                  <Input
                    type="password"
                    name="newPassword"
                    value={passwordForm.newPassword}
                    onChange={(e) =>
                      setPasswordForm((prev) => ({
                        ...prev,
                        newPassword: e.target.value,
                      }))
                    }
                    className="focus-visible:ring-[#0052CC]"
                  />
                </div>
                <div className="flex justify-end pt-4">
                  <Button
                    type="submit"
                    disabled={passwordSaving}
                    className="bg-[#0052CC] text-white hover:bg-[#0747A6]"
                  >
                    {passwordSaving ? "Updating…" : "Update Password"}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-[#172B4D]">Email</CardTitle>
              <CardDescription>Request an email change</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleEmailSubmit} className="space-y-4">
                <div>
                  <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                    New Email
                  </label>
                  <Input
                    type="email"
                    name="newEmail"
                    value={emailForm.newEmail}
                    onChange={(e) =>
                      setEmailForm({ newEmail: e.target.value })
                    }
                    className="focus-visible:ring-[#0052CC]"
                  />
                </div>
                <div className="flex justify-end pt-4">
                  <Button
                    type="submit"
                    disabled={emailRequesting}
                    className="bg-[#0052CC] text-white hover:bg-[#0747A6]"
                  >
                    {emailRequesting ? "Requesting…" : "Request Email Change"}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-[#172B4D]">Deactivate Account</CardTitle>
              <CardDescription>Disable your account access</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleDeactivateSubmit} className="space-y-4">
                <div>
                  <label className="text-sm font-semibold text-[#172B4D] mb-1 block">
                    Current Password
                  </label>
                  <Input
                    type="password"
                    name="currentPassword"
                    value={deactivateForm.currentPassword}
                    onChange={(e) =>
                      setDeactivateForm({ currentPassword: e.target.value })
                    }
                    className="focus-visible:ring-[#0052CC]"
                  />
                </div>
                <div className="flex justify-end pt-4">
                  <Button
                    type="submit"
                    disabled={deactivating}
                    className="bg-[#DE350B] text-white hover:bg-[#BF2600]"
                  >
                    {deactivating ? "Deactivating…" : "Deactivate Account"}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default page;
