package farnetto.log4jconverter;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Contains options to control conversion
 *
 * Can be parsed from args
 */
public class ConverterConfig
{
    private static final Logger LOG = LogManager.getLogger(ConverterConfig.class);

    private static final Pattern flagPattern = Pattern.compile("^--?.+$");

    private final List<FileObject> xmlFiles = new ArrayList<>();

    private final List<FileObject> propFiles = new ArrayList<>();

    private String workingDir = System.getProperty("user.dir");
    private boolean inPlace = false;

    private boolean findAll = false;

    private String convertPropertiesPattern = null;

    public static ConverterConfig fromArgs(String[] args)
        throws FileNotFoundException, FileSystemException {

        FileSystemManager fileSystemManager = VFS.getManager();
        FileSystemOptions fileSystemOptions = new FileSystemOptions();
        SftpFileSystemConfigBuilder.getInstance()
            .setUserDirIsRoot(fileSystemOptions, false);

        ConverterConfig config = new ConverterConfig();

        Arrays.stream(args).filter(flagPattern.asPredicate()).distinct()
            .forEach(arg -> {
                final String[] flagToVal = arg.split("=", 2);
                String flag = flagToVal[0];
                Optional<String> value = Optional.ofNullable(
                    flagToVal.length > 1 ? flagToVal[1] : null);

                switch (flag) {
                case "-h":
                case "-?":
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;
                case "-i":
                case "--in-place":
                    config.inPlace = true;
                    break;
                case "-a":
                case "--all":
                case "--find-all":
                    config.inPlace = true;
                    config.findAll = true;
                    break;
                case "-w":
                case "--wd":
                case "--workdir":
                case "--working-directory":
                    if (!value.isPresent()) {
                        System.err.println("Specify working directory as -w=<path>");
                        printHelp();
                        System.exit(1);
                    }
                    config.workingDir = value.get();
                    break;
                case "-p":
                case "--properties":
                case "--convert-properties":
                    config.convertPropertiesPattern = value
                        .orElse(".*/log4j(?!2).*\\.properties");
                    break;
                default:
                    System.err.println("Unknown option: " + flag);
                    printHelp();
                    System.exit(1);
                }
            });

        final List<String> nonFlagArgs = Arrays.stream(args)
            .filter(flagPattern.asPredicate().negate()).distinct().collect(
                Collectors.toList());

        if (!config.findAll) {
            if (nonFlagArgs.isEmpty()) {
                System.err.println(
                    "input file(s) must be specified if --find-all "
                        + "is not used");
                printHelp();
                System.exit(1);
            }
            if (!config.inPlace && nonFlagArgs.size() > 1) {
                System.out.println(
                    "multiple files are specified, so --in-place is activated");
                config.inPlace = true;
            }
            // Interpret args as file(s)
            nonFlagArgs.forEach(file -> {
                try {
                    final FileObject fileObject = fileSystemManager.resolveFile(
                        file, fileSystemOptions);
                    if (fileObject.exists() && fileObject.isFile()) {
                        config.xmlFiles.add(fileObject);
                    } else {
                        System.err.println("File not found: " + file);
                    }
                } catch (FileSystemException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            // Search for patterns in current dir
            final FileObject curDir = fileSystemManager.resolveFile(
                config.getWorkingDir(), fileSystemOptions);
            List<String> patterns = new ArrayList<>(nonFlagArgs);
            if (patterns.isEmpty()) {
                // Use default pattern
                patterns.add(".*/log4j(?!2).*\\.xml");
            }
            patterns.forEach(pattern -> {
                try {
                    Arrays.stream(
                            curDir.findFiles(new PatternFileSelector(pattern)))
                        .forEach(fileObject -> {
                            try {
                                if (fileObject.isFile())
                                    config.xmlFiles.add(fileObject);
                            } catch (FileSystemException e) {
                                throw new RuntimeException(e);
                            }
                        });
                } catch (FileSystemException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        if (config.getConvertPropertiesPattern() != null) {
            final FileObject curDir = fileSystemManager.resolveFile(
                config.getWorkingDir(), fileSystemOptions);
            try {
                Arrays.stream(
                        curDir.findFiles(new PatternFileSelector(
                            config.getConvertPropertiesPattern())))
                    .forEach(fileObject -> {
                        try {
                            if (fileObject.isFile())
                                config.propFiles.add(fileObject);
                        } catch (FileSystemException e) {
                            throw new RuntimeException(e);
                        }
                    });
            } catch (FileSystemException e) {
                throw new RuntimeException(e);
            }
        }

        return config;
    }

    private static void printHelp() {
        System.out.println("\nConverts Log4j XML config file(s) to the Log4j2 format.");
        System.out.println("Without options, a config file is expected as input and the converted config is printed to stdout (legacy behavior).");
        System.out.println("If multiple files are passed, -i is implied. Use -a for batch find-and-convert.\n");
        System.out.println("Options:");
        System.out.println("-h, -?, --help\t\t\t\t\t\t\t\tShow this help");
        System.out.println("-i, --in-place\t\t\t\t\t\t\t\tConvert the given config file(s) inside their current folder, appending a '2' to the filename after 'log4j' or before the extension if not present");
        System.out.println("-a, --all, --find-all\t\t\t\t\t\tConvert all found configs matching the given pattern(s) (default if omitted: .*/log4j(?!2).*\\.xml), implies -i");
        System.out.println("-w=<path>, --workdir=<path>");
        System.out.println("--wd=<path>, --working-directory=<path>\t\tSet working directory, default is the current directory, only relevant for -a");
    }

    public List<FileObject> getXmlFiles() {
        return xmlFiles;
    }

    public Boolean isInPlace() {
        return inPlace;
    }

    public List<FileObject> getPropFiles() {
        return propFiles;
    }

    public Boolean isFindAll() {
        return findAll;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public String getConvertPropertiesPattern() {
        return convertPropertiesPattern;
    }
}
