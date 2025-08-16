import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SafeFileOrganizer
{

    private static final String LOG_FILE = "organizer.log";

    private static final Map<String, List<String>> FILE_CATEGORIES = Map.of(
        "Images", List.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".icon"),
        "Videos", List.of(".mp4", ".mov", ".mkv", ".avi"),
        "Audio", List.of(".mp3", ".flp", ".wav", ".aup"),
        "Documents", List.of(".docx", ".pdf", ".pptx", ".csv", ".txt", "doc", ".ppt", "doc"),
        "Archives", List.of(".zip", ".rar", ".7z", ".tar", ".gz", ".sitx"),
        "Executables", List.of(".jar", ".sh", ".bat")
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        ".exe", ".dll", ".sys", ".app", ".deb", ".msi"
    );

    private static final List<String> SAFE_DIRECTORIES = List.of(
        System.getProperty("user.home") + "/Desktop",
        System.getProperty("user.home") + "/Downloads" 
        //<------------> Can add more here <------------>
    );

    private static final List<String> DANGEROUS_PATH_PREFIXES = List.of(
        "/bin", "/etc", "/lib", "/usr", "/sbin", "/System", "/opt"
    );

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter folder path to organize (must be whitelisted):");
        String inputPath = scanner.nextLine().trim();
        if (!isPathSafe(inputPath)) {
            System.out.println("Path is unsafe or not whitelisted. Exiting.");
            return;
        }
        if (!isPathSafe(inputPath)) {
            System.out.println("Rejected: Either the folder is outside allowed directories or is a system folder.");
            return;
        }
        File folder = new File(inputPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid directory.");
            return;
        }
        //Basically what this section does is it makes sure that it'll print out whatever that's gonna be moved.
        System.out.println("Do a dry run first? (Y/n):");
        boolean dryRun = scanner.nextLine().trim().equalsIgnoreCase("Y");
        Map<String, List<String>> customCategories = getCustomCategories(scanner);
        // Merge default and custom categories
        Map<String, List<String>> allCategories = new HashMap<>(FILE_CATEGORIES);
        allCategories.putAll(customCategories);
        organizeFiles(new File(inputPath), dryRun, allCategories);
    }

    private static Map<String, List<String>> getCustomCategories(Scanner scanner) {
        Map<String, List<String>> customCategories = new HashMap<>();

        System.out.println("Do you want to add custom categories? (Y/n):");
        String answer = scanner.nextLine().trim();

        while (answer.equalsIgnoreCase("Y")) {
            System.out.println("Enter folder name for the new category:");
            String folderName = scanner.nextLine().trim();

            System.out.println("Enter file extensions for this category (comma separated, e.g. .pdf,.docx):");
            String extensionsInput = scanner.nextLine().trim();

            // Parse extensions into list.
            List<String> extensions = new ArrayList<>();
            for (String ext : extensionsInput.split(",")) {
                ext = ext.trim().toLowerCase();
                if (!ext.startsWith(".")) {
                    ext = "." + ext;
                }
                extensions.add(ext);
            }

            customCategories.put(folderName, extensions);

            System.out.println("Add another custom category? (Y/n):");
            answer = scanner.nextLine().trim();
        }

        return customCategories;
    }


    private static void organizeFiles(File folder, boolean dryRun, Map<String, List<String>> categories) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            try {
                if (!file.isFile() || Files.isHidden(file.toPath())) continue;
                String extension = getFileExtension(file.getName()).toLowerCase();
                if (BLOCKED_EXTENSIONS.contains(extension)) continue;
                boolean moved = false;
                for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
                    if (entry.getValue().contains(extension)) {
                        if (dryRun) {
                            System.out.println("[DRY RUN] Would move " + file.getName() + " → " + entry.getKey() + "/");
                        } else {
                            moveFileToCategory(file, folder, entry.getKey());
                        }
                        moved = true;
                        break;
                    }
                }
                if (!moved) {
                    if (dryRun) {
                        System.out.println("[DRY RUN] Would move " + file.getName() + " → Others/");
                    } else {
                        moveFileToCategory(file, folder, "Others");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error processing file: " + file.getName());
            }
        }
        if (dryRun) {
            System.out.println("Dry run complete. No files were moved.");
        }else {
            System.out.println("File organization complete.");
        }
    }


    private static void moveFileToCategory(File file, File baseFolder, String category) throws IOException {
        File categoryDir = new File(baseFolder, category);
        if (!categoryDir.exists()) categoryDir.mkdirs();
        Path targetPath = categoryDir.toPath().resolve(file.getName());
        Files.move(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logMove(file, targetPath.toFile());
        System.out.println("Moved " + file.getName() + " → " + category + "/");
    }

    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(dotIndex) : "";
    }

        private static boolean isPathSafe(String inputPath) {
        try {
            Path canonicalInput = new File(inputPath).getCanonicalFile().toPath();
            // Check whether if it is under whitelisted base directory.
            for (String safe : SAFE_DIRECTORIES) {
                Path safePath = new File(safe).getCanonicalFile().toPath(); //Canonical Path is the fully resolved, absolute, unique path path.
                if (canonicalInput.startsWith(safePath)) {
                    // Also check it doesn't fall into a system path
                    for (String danger : DANGEROUS_PATH_PREFIXES) {
                        if (canonicalInput.toString().startsWith(danger)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error validating path: " + e.getMessage());
        }

        return false;
    }

    private static void logMove(File source, File target) {
    try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        out.printf("[%s] Moved: %s → %s%n", timestamp, source.getAbsolutePath(), target.getAbsolutePath());
    } catch (IOException e) {
        System.err.println("Failed to write to log: " + e.getMessage());
    }
    }
}