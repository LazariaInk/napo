import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const API_BASE = "";
const DEFAULT_CREDENTIALS = {
  email: "test@napo.local",
  password: "password123"
};

function authHeader(credentials) {
  if (!credentials?.email || !credentials?.password) {
    return {};
  }

  return {
    Authorization: `Basic ${btoa(`${credentials.email}:${credentials.password}`)}`
  };
}

async function apiRequest(path, { method = "GET", body, credentials } = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...authHeader(credentials)
    },
    body: body ? JSON.stringify(body) : undefined
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(data?.message || `Request failed with status ${response.status}`);
  }

  return data;
}

function App() {
  const [theme, setTheme] = useState(() => localStorage.getItem("napo-theme") || "dark");
  const [credentials, setCredentials] = useState(() => {
    const saved = localStorage.getItem("napo-credentials");
    return saved ? JSON.parse(saved) : DEFAULT_CREDENTIALS;
  });
  const [user, setUser] = useState(null);
  const [view, setView] = useState("chat");
  const [reminders, setReminders] = useState([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("napo-theme", theme);
  }, [theme]);

  useEffect(() => {
    localStorage.setItem("napo-credentials", JSON.stringify(credentials));
  }, [credentials]);

  async function loadReminders(activeCredentials = credentials) {
    const data = await apiRequest("/api/reminders", { credentials: activeCredentials });
    setReminders(data || []);
  }

  async function handleAuthenticated(authUser, activeCredentials) {
    setUser(authUser);
    setCredentials(activeCredentials);
    await loadReminders(activeCredentials);
  }

  async function runAction(action) {
    setError("");
    setIsLoading(true);
    try {
      await action();
    } catch (caughtError) {
      setError(caughtError.message);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar
        view={view}
        setView={setView}
        user={user}
        reminderCount={reminders.length}
        recurringCount={reminders.filter((reminder) => reminder.recurrenceType !== "NONE").length}
      />

      <main className="main-panel">
        <TopBar
          theme={theme}
          setTheme={setTheme}
          user={user}
          onRefresh={() => runAction(loadReminders)}
        />

        {error && (
          <div className="notice error">
            <span>{error}</span>
            <button type="button" onClick={() => setError("")}>Close</button>
          </div>
        )}

        {!user ? (
          <AuthPanel
            credentials={credentials}
            setCredentials={setCredentials}
            onAuthenticated={(authUser, activeCredentials) =>
              runAction(() => handleAuthenticated(authUser, activeCredentials))
            }
            isLoading={isLoading}
          />
        ) : (
          <>
            {view === "chat" && (
              <ChatPanel
                credentials={credentials}
                reminders={reminders}
                onReminderCreated={() => runAction(loadReminders)}
                isLoading={isLoading}
              />
            )}
            {view === "reminders" && (
              <ReminderStack
                credentials={credentials}
                reminders={reminders}
                onChanged={() => runAction(loadReminders)}
              />
            )}
            {view === "recurring" && (
              <ReminderStack
                title="Recurentele mele"
                credentials={credentials}
                reminders={reminders.filter((reminder) => reminder.recurrenceType !== "NONE")}
                onChanged={() => runAction(loadReminders)}
              />
            )}
            {view === "calendar" && <CalendarView reminders={reminders} />}
            {view === "settings" && (
              <SettingsPanel
                theme={theme}
                setTheme={setTheme}
                credentials={credentials}
                setCredentials={setCredentials}
                user={user}
              />
            )}
          </>
        )}
      </main>
    </div>
  );
}

function Sidebar({ view, setView, user, reminderCount, recurringCount }) {
  const items = [
    ["chat", "Chat", "⌘"],
    ["reminders", `Remindere (${reminderCount})`, "□"],
    ["recurring", `Recurente (${recurringCount})`, "↻"],
    ["calendar", "Calendar", "◷"],
    ["settings", "Setari", "⚙"]
  ];

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">N</div>
        <div>
          <strong>NAPO</strong>
          <span>Reminder assistant</span>
        </div>
      </div>

      <nav className="nav-list">
        {items.map(([id, label, icon]) => (
          <button
            key={id}
            type="button"
            className={view === id ? "active" : ""}
            onClick={() => setView(id)}
          >
            <span>{icon}</span>
            {label}
          </button>
        ))}
      </nav>

      <div className="sidebar-footer">
        <span>{user ? user.email : "Not signed in"}</span>
      </div>
    </aside>
  );
}

function TopBar({ theme, setTheme, user, onRefresh }) {
  return (
    <header className="topbar">
      <div>
        <h1>NAPO</h1>
        <p>{user ? "Gestioneaza reminderele prin conversatie." : "Conecteaza-te pentru testare."}</p>
      </div>
      <div className="topbar-actions">
        {user && <button type="button" className="ghost-button" onClick={onRefresh}>Refresh</button>}
        <button
          type="button"
          className="theme-toggle"
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          title="Toggle theme"
        >
          {theme === "dark" ? "☾" : "☼"}
        </button>
      </div>
    </header>
  );
}

function AuthPanel({ credentials, setCredentials, onAuthenticated, isLoading }) {
  const [displayName, setDisplayName] = useState("Test User");
  const [mode, setMode] = useState("login");

  async function submit(event) {
    event.preventDefault();
    const activeCredentials = {
      email: credentials.email.trim(),
      password: credentials.password
    };

    const user = await apiRequest(`/api/auth/${mode}`, {
      method: "POST",
      credentials: activeCredentials,
      body:
        mode === "register"
          ? { ...activeCredentials, displayName }
          : activeCredentials
    });

    onAuthenticated(user, activeCredentials);
  }

  return (
    <section className="auth-layout">
      <div className="auth-copy">
        <span className="eyebrow">Local MVP</span>
        <h2>Testeaza NAPO cu userul tau local.</h2>
        <p>
          Frontend-ul foloseste Basic Auth si proxy Vite catre Spring Boot. Dupa login,
          poti crea remindere din chat, le poti edita si le poti vedea in calendar.
        </p>
      </div>

      <form className="panel auth-panel" onSubmit={submit}>
        <div className="segmented">
          <button type="button" className={mode === "login" ? "selected" : ""} onClick={() => setMode("login")}>
            Login
          </button>
          <button type="button" className={mode === "register" ? "selected" : ""} onClick={() => setMode("register")}>
            Register
          </button>
        </div>

        <label>
          Email
          <input
            value={credentials.email}
            onChange={(event) => setCredentials({ ...credentials, email: event.target.value })}
            autoComplete="username"
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={credentials.password}
            onChange={(event) => setCredentials({ ...credentials, password: event.target.value })}
            autoComplete={mode === "login" ? "current-password" : "new-password"}
          />
        </label>
        {mode === "register" && (
          <label>
            Display name
            <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
          </label>
        )}
        <button type="submit" className="primary-button" disabled={isLoading}>
          {isLoading ? "Se proceseaza..." : mode === "login" ? "Login" : "Creeaza cont"}
        </button>
      </form>
    </section>
  );
}

function ChatPanel({ credentials, onReminderCreated }) {
  const [message, setMessage] = useState("Creeaza-mi un reminder pe 23 august 2026 la ora 10 sa ii spun lui Cristi: La multi ani. Sa fie anual.");
  const [conversationId, setConversationId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [isSending, setIsSending] = useState(false);

  async function sendMessage(event) {
    event.preventDefault();
    if (!message.trim()) {
      return;
    }

    const userText = message.trim();
    setMessage("");
    setMessages((current) => [...current, { sender: "user", content: userText }]);
    setIsSending(true);

    try {
      const response = await apiRequest("/api/chat/messages", {
        method: "POST",
        credentials,
        body: { conversationId, message: userText }
      });
      setConversationId(response.conversationId);
      setMessages((current) => [
        ...current,
        {
          sender: "assistant",
          content: response.assistantMessage,
          reminder: response.reminder,
          type: response.type
        }
      ]);
      if (response.reminder) {
        await onReminderCreated();
      }
    } catch (error) {
      setMessages((current) => [...current, { sender: "assistant", content: error.message, type: "error" }]);
    } finally {
      setIsSending(false);
    }
  }

  return (
    <section className="chat-layout">
      <div className="chat-thread">
        {messages.length === 0 ? (
          <div className="empty-state">
            <span>NAPO Chat</span>
            <h2>Scrie natural ce vrei sa iti aminteasca.</h2>
            <p>Exemplu: zi de nastere, task, follow-up, eveniment recurent.</p>
          </div>
        ) : (
          messages.map((item, index) => (
            <div key={`${item.sender}-${index}`} className={`message ${item.sender}`}>
              <p>{item.content}</p>
              {item.reminder && (
                <div className="mini-reminder">
                  <strong>{item.reminder.title}</strong>
                  <span>{formatDate(item.reminder.remindAt)}</span>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      <form className="composer" onSubmit={sendMessage}>
        <textarea
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          placeholder="Scrie un reminder..."
          rows={3}
        />
        <button type="submit" className="primary-button" disabled={isSending}>
          {isSending ? "Trimite..." : "Send"}
        </button>
      </form>
    </section>
  );
}

function ReminderStack({ title = "Reminderele mele", credentials, reminders, onChanged }) {
  const [editing, setEditing] = useState(null);

  async function deleteReminder(id) {
    await apiRequest(`/api/reminders/${id}`, {
      method: "DELETE",
      credentials
    });
    await onChanged();
  }

  async function cancelReminder(id) {
    await apiRequest(`/api/reminders/${id}/cancel`, {
      method: "PATCH",
      credentials
    });
    await onChanged();
  }

  return (
    <section className="content-section">
      <div className="section-header">
        <div>
          <h2>{title}</h2>
          <p>{reminders.length} remindere gasite</p>
        </div>
      </div>

      <div className="stack-list">
        {reminders.length === 0 ? (
          <div className="panel muted-panel">Nu ai remindere in aceasta sectiune.</div>
        ) : (
          reminders.map((reminder) => (
            <article className="reminder-card" key={reminder.id}>
              <div className="reminder-main">
                <span className={`status-dot ${reminder.status.toLowerCase()}`} />
                <div>
                  <h3>{reminder.title}</h3>
                  <p>{reminder.description || reminder.messageToSend || "Fara descriere"}</p>
                  <div className="meta-row">
                    <span>{formatDate(reminder.remindAt)}</span>
                    <span>{reminder.recurrenceType}</span>
                    <span>{reminder.preferredChannel}</span>
                    <span>{reminder.status}</span>
                  </div>
                </div>
              </div>
              <div className="card-actions">
                <button type="button" onClick={() => setEditing(reminder)}>Edit</button>
                <button type="button" onClick={() => cancelReminder(reminder.id)}>Cancel</button>
                <button type="button" className="danger" onClick={() => deleteReminder(reminder.id)}>Delete</button>
              </div>
            </article>
          ))
        )}
      </div>

      {editing && (
        <EditReminderModal
          reminder={editing}
          credentials={credentials}
          onClose={() => setEditing(null)}
          onSaved={async () => {
            setEditing(null);
            await onChanged();
          }}
        />
      )}
    </section>
  );
}

function EditReminderModal({ reminder, credentials, onClose, onSaved }) {
  const [form, setForm] = useState(() => ({
    title: reminder.title,
    description: reminder.description || "",
    remindAt: toDatetimeLocal(reminder.remindAt),
    timezone: reminder.timezone || "Europe/Bucharest",
    status: reminder.status,
    recurrenceType: reminder.recurrenceType,
    preferredChannel: reminder.preferredChannel,
    messageToSend: reminder.messageToSend || ""
  }));

  async function submit(event) {
    event.preventDefault();
    await apiRequest(`/api/reminders/${reminder.id}`, {
      method: "PUT",
      credentials,
      body: {
        ...form,
        remindAt: fromDatetimeLocal(form.remindAt),
        recurrenceRule: null
      }
    });
    await onSaved();
  }

  return (
    <div className="modal-backdrop">
      <form className="modal panel" onSubmit={submit}>
        <div className="section-header">
          <h2>Edit reminder</h2>
          <button type="button" className="ghost-button" onClick={onClose}>Close</button>
        </div>

        <label>
          Title
          <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
        </label>
        <label>
          Description
          <textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
        </label>
        <label>
          Date and time
          <input type="datetime-local" value={form.remindAt} onChange={(event) => setForm({ ...form, remindAt: event.target.value })} />
        </label>
        <div className="form-grid">
          <label>
            Status
            <select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>
              {["PENDING", "TRIGGERED", "COMPLETED", "CANCELLED", "FAILED"].map((value) => <option key={value}>{value}</option>)}
            </select>
          </label>
          <label>
            Recurenta
            <select value={form.recurrenceType} onChange={(event) => setForm({ ...form, recurrenceType: event.target.value })}>
              {["NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"].map((value) => <option key={value}>{value}</option>)}
            </select>
          </label>
          <label>
            Canal
            <select value={form.preferredChannel} onChange={(event) => setForm({ ...form, preferredChannel: event.target.value })}>
              {["LOG", "IN_APP"].map((value) => <option key={value}>{value}</option>)}
            </select>
          </label>
        </div>
        <label>
          Message
          <textarea value={form.messageToSend} onChange={(event) => setForm({ ...form, messageToSend: event.target.value })} />
        </label>
        <button type="submit" className="primary-button">Save</button>
      </form>
    </div>
  );
}

function CalendarView({ reminders }) {
  const grouped = useMemo(() => {
    return reminders.reduce((acc, reminder) => {
      const key = reminder.remindAt.slice(0, 10);
      acc[key] = [...(acc[key] || []), reminder];
      return acc;
    }, {});
  }, [reminders]);

  const days = buildCalendarDays(new Date());

  return (
    <section className="content-section">
      <div className="section-header">
        <div>
          <h2>Calendar</h2>
          <p>Vizualizare rapida pentru luna curenta</p>
        </div>
      </div>

      <div className="calendar-grid">
        {days.map((day) => {
          const key = day.toISOString().slice(0, 10);
          const dayReminders = grouped[key] || [];
          return (
            <div className={`calendar-day ${isToday(day) ? "today" : ""}`} key={key}>
              <strong>{day.getDate()}</strong>
              {dayReminders.slice(0, 3).map((reminder) => (
                <span key={reminder.id}>{reminder.title}</span>
              ))}
            </div>
          );
        })}
      </div>
    </section>
  );
}

function SettingsPanel({ theme, setTheme, credentials, setCredentials, user }) {
  return (
    <section className="content-section settings-grid">
      <div className="panel">
        <h2>Appearance</h2>
        <p className="muted">Tema este salvata local in browser.</p>
        <div className="segmented wide">
          <button type="button" className={theme === "dark" ? "selected" : ""} onClick={() => setTheme("dark")}>Dark</button>
          <button type="button" className={theme === "light" ? "selected" : ""} onClick={() => setTheme("light")}>Light</button>
        </div>
      </div>
      <div className="panel">
        <h2>API session</h2>
        <p className="muted">{user?.email}</p>
        <label>
          Email
          <input value={credentials.email} onChange={(event) => setCredentials({ ...credentials, email: event.target.value })} />
        </label>
        <label>
          Password
          <input type="password" value={credentials.password} onChange={(event) => setCredentials({ ...credentials, password: event.target.value })} />
        </label>
      </div>
    </section>
  );
}

function buildCalendarDays(date) {
  const year = date.getFullYear();
  const month = date.getMonth();
  const first = new Date(year, month, 1);
  const start = new Date(first);
  start.setDate(first.getDate() - ((first.getDay() + 6) % 7));
  return Array.from({ length: 42 }, (_, index) => {
    const day = new Date(start);
    day.setDate(start.getDate() + index);
    return day;
  });
}

function isToday(date) {
  const today = new Date();
  return date.toDateString() === today.toDateString();
}

function formatDate(value) {
  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

function toDatetimeLocal(value) {
  const date = new Date(value);
  const offset = date.getTimezoneOffset();
  const localDate = new Date(date.getTime() - offset * 60_000);
  return localDate.toISOString().slice(0, 16);
}

function fromDatetimeLocal(value) {
  return new Date(value).toISOString();
}

createRoot(document.getElementById("root")).render(<App />);
