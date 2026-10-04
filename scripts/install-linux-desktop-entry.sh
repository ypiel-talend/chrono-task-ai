#!/usr/bin/env bash
# Installs a desktop entry + icon for the current user so GNOME/KDE show the Chrono Task AI icon
# in the dock, taskbar and Alt-Tab (they take it from the .desktop file matched by StartupWMClass,
# not from the window). Re-run after moving the repository.
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
icons="$repo/src/main/resources/com/chrono/task/icons"
data_home="${XDG_DATA_HOME:-$HOME/.local/share}"
# Desktop launchers do not get the shell PATH (sdkman), so pin mvn and the JDK as resolved now
mvn_bin="$(command -v mvn)"
java_home="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}"

for size in 16 24 32 48 64 128 256 512; do
    install -Dm644 "$icons/app-icon-$size.png" "$data_home/icons/hicolor/${size}x${size}/apps/chrono-task-ai.png"
done
install -Dm644 "$icons/app-icon.svg" "$data_home/icons/hicolor/scalable/apps/chrono-task-ai.svg"

install -d "$data_home/applications"
cat > "$data_home/applications/chrono-task-ai.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=Chrono Task AI
Comment=Daily tasks, time tracking and notes
Icon=chrono-task-ai
Exec=env JAVA_HOME=$java_home $mvn_bin -q javafx:run
Path=$repo
Terminal=false
Categories=Office;ProjectManagement;
StartupWMClass=com.chrono.task.ChronoApp
EOF

command -v gtk-update-icon-cache >/dev/null && gtk-update-icon-cache -q -t "$data_home/icons/hicolor" || true
command -v update-desktop-database >/dev/null && update-desktop-database -q "$data_home/applications" || true
echo "Installed $data_home/applications/chrono-task-ai.desktop (restart the app to see the icon)."
