import React from 'react'
import { Routes, Route } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute.jsx'

import Landing from './pages/Landing.jsx'
import VerifyResult from './pages/VerifyResult.jsx'
import UploadVerify from './pages/UploadVerify.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'

import IssuerOverview from './pages/issuer/IssuerOverview.jsx'
import IssueCredential from './pages/issuer/IssueCredential.jsx'
import CredentialsList from './pages/issuer/CredentialsList.jsx'
import CredentialDetail from './pages/issuer/CredentialDetail.jsx'

import PendingIssuers from './pages/admin/PendingIssuers.jsx'
import AllIssuers from './pages/admin/AllIssuers.jsx'
import VerificationLogs from './pages/admin/VerificationLogs.jsx'
import StudentCredentials from './pages/student/StudentCredentials.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/verify/:credentialId" element={<VerifyResult />} />
      <Route path="/verify-upload" element={<UploadVerify />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route path="/issuer" element={<ProtectedRoute role="ISSUER"><IssuerOverview /></ProtectedRoute>} />
      <Route path="/issuer/issue" element={<ProtectedRoute role="ISSUER"><IssueCredential /></ProtectedRoute>} />
      <Route path="/issuer/credentials" element={<ProtectedRoute role="ISSUER"><CredentialsList /></ProtectedRoute>} />
      <Route path="/issuer/credentials/:id" element={<ProtectedRoute role="ISSUER"><CredentialDetail /></ProtectedRoute>} />

      <Route path="/admin" element={<ProtectedRoute role="ADMIN"><PendingIssuers /></ProtectedRoute>} />
      <Route path="/admin/issuers" element={<ProtectedRoute role="ADMIN"><AllIssuers /></ProtectedRoute>} />
      <Route path="/admin/logs" element={<ProtectedRoute role="ADMIN"><VerificationLogs /></ProtectedRoute>} />

      <Route path="/student" element={<ProtectedRoute role="STUDENT"><StudentCredentials /></ProtectedRoute>} />

      <Route path="*" element={<Landing />} />
    </Routes>
  )
}
