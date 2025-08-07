--------------Safe Auto File Organizer (Java)--------------

A simple, safe Java-based utility that automatically sorts files in a given folder into categorized subfolders — like `Images`, `Documents`, `Videos`, etc.
It avoids modifying system files, application folders, or sensitive file types. Great for cleaning up your Desktop or Downloads folder without risk.

---------------------------------------------------------

Features

- Automatically moves files into categories:
  - Images, Videos, Documents, Archives, Executables, Others
- Safety-first:
  - Only processes whitelisted folders
  - Ignores hidden files and system folders
  - Blocks dangerous extensions (like `.dll`, `.sys`)
- Logs every file move to `organizer.log`
- Dry-run mode for previewing actions before applying
