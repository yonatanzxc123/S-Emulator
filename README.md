# S-Emulator

**S-Emulator** is a powerful Java-based emulator for the theoretical programming language "S"—a minimalist language designed to explore computability with only a few basic instructions. This project includes both console and graphical user interfaces, as well as a full client-server deployment with support for multiple users, function handling, and program execution tracking.

---

## 🔍 What Is S Language?

S is a theoretical computation model used in computer science to demonstrate how a minimal set of operations can achieve Turing completeness. S contains only **4 primitive instructions**:

1. `V ← V + 1` — Increment a variable.
2. `V ← V - 1` — Decrement a variable (floored at 0).
3. `IF V ≠ 0 GOTO L` — Conditional jump to a label.
4. `V ← V` — A no-op instruction.

From this minimal core, S-Emulator enables building **syntactic sugars**, **function compositions**, and even complex program structures.

---

## 📦 Project Structure & Evolution

The project is structured into **three major parts**, each building on the previous one:

### ✅ Part 1 – Core S Emulator (Console)
- Implements core S language behavior.
- Loads and executes XML-defined S programs.
- Tracks state of all program variables (`x_i`, `z_i`, `y`).
- Expands synthetic instructions (mimicking compilation).
- Includes a simple console menu (1–9 options) to load, run, and inspect programs.

### 🎛️ Part 2 – JavaFX Graphical UI
- Adds a modern GUI using JavaFX.
- Visualizes program instruction tables, variable states, and runtime metrics.
- Features:
  - Input tables with editable parameters.
  - History tracking of runs.
  - On-screen debugging and step-by-step control.
  - Expand/collapse logic per instruction degree.
- Internal structure includes:
  - FXML + Controller + CSS for each UI component.
  - Header (file loader & path display).
  - Center section with all runtime tables.
  - Modular program selector, debug controls, and visualization helpers.

### 🌐 Part 3 – Server & Remote Execution (Tomcat)
- Adds support for multiple users and remote execution via HTTP API.
- Built on Jakarta Servlet API and deployable on Apache Tomcat.
- Stores state and functions in memory for all logged-in users.
- Supports program upload, execution, credit-based control, and state history.

---

## ⚙️ Core Features

### ✅ Program Execution
- Runs S programs at selected **expansion degree**.
- Uses **FunctionEnv** context to resolve function dependencies.
- Tracks **cycle count** for performance cost estimation.

### 🧠 Synthetic Instructions & Expansion
- Implements custom syntactic sugars such as:
  - `ZeroVariable`
  - `ConstantAssignment`
  - `JumpEqualFunction`
  - `Quote` (meta-programming via nested calls)
- Each synthetic instruction can be expanded recursively into basic S instructions.

### 🧪 Debugging & History
- Step-by-step run control.
- Variable state inspection.
- Historical record of each run with inputs/outputs.

### 🧰 Function Environment
- Functions are represented as S programs.
- **FunctionEnv** is a thread-local context manager for runtime/expansion resolution.
- Supports recursion, name mapping, and safe expansion hygiene.

### 💳 Credit-Based Execution
- Each run deducts credits based on:
  - Architecture tier (I–IV).
  - Instruction cycle count.
- Server rejects execution if credits are insufficient.
- Server tracks per-user usage, uploads, and runs.

---

## 🔗 HTTP API Overview (Tomcat Server)

| Endpoint                  | Method | Description                          |
|--------------------------|--------|--------------------------------------|
| `/api/programs/upload`   | POST   | Upload new program XML               |
| `/api/programs/functions`| GET    | List all available functions         |
| `/api/programs/:name/body` | GET | Get program instructions by name     |
| `/api/run/start`         | POST   | Start execution with inputs & degree |
| `/api/run/inputs`        | POST   | Fetch input variable names           |
| `/api/arch/summary`      | GET    | Get architecture tier usage summary  |

### Example

```json
POST /api/run/start
{
  "program": "MyAdder",
  "arch": "II",
  "degree": 3,
  "inputs": [5, 3]
}
```

Response:
```json
{
  "ok": true,
  "cycles": 12,
  "y": 8,
  "creditsLeft": 488,
  "vars": { "x1": 5, "x2": 3, "y": 8 }
}
```

---

## 🖥️ How to Run Locally

### Requirements
- Java 17+
- JavaFX SDK (for GUI)
- Jakarta Servlet JARs (for server)

### Console Mode
```bash
cd S-Emulator
build.bat
run.bat
```

### JavaFX UI
```bash
javac --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  -cp dist/engine.jar -d build/javafx JavaFX/src/**/*.java
java --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  -jar dist/javafx-client.jar
```

### Tomcat Server Deployment
1. Build server into `server/WEB-INF/classes`
2. Include `web.xml` in `WEB-INF/`
3. Deploy to Tomcat at `localhost:8080/server`

### TomcatClient UI
```bash
javac --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  -cp . TomcatClient/src/**/*.java
java --module-path /path/to/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  -Dapi.base=http://localhost:8080/server \
  -cp . ui.Main
```

---

## 🧩 Project Highlights
- **Clean Modular Design**: Console, JavaFX, Server separated.
- **Thread-Local FunctionEnv**: Elegant runtime context resolution.
- **Program Expansion**: Degree-based unrolling of synthetic instructions.
- **Interactive UI**: Debugging, inputs, memory, and expansions visualized.
- **Server Mode**: Remote program management, execution, and credit system.

---

## 🧠 Educational Value
This project was inspired by theoretical foundations in **Computability Theory**. By implementing a system that models a Turing-complete language with just 3–4 instructions, it provides a unique learning experience that bridges **low-level programming**, **compilers**, **meta-programming**, and **distributed systems**.

