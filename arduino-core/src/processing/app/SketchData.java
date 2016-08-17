package processing.app;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static processing.app.I18n.tr;

public class SketchData {

  public static final List<String> SKETCH_EXTENSIONS = Arrays.asList("ino", "pde");
  public static final List<String> OTHER_ALLOWED_EXTENSIONS = Arrays.asList("c", "cpp", "h", "hh", "hpp", "s");
  public static final List<String> EXTENSIONS = Stream.concat(SKETCH_EXTENSIONS.stream(), OTHER_ALLOWED_EXTENSIONS.stream()).collect(Collectors.toList());

  /**
   * main pde file for this sketch.
   */
  private File primaryFile;

  /**
   * folder that contains this sketch
   */
  private File folder;

  /**
   * data folder location for this sketch (may not exist yet)
   */
  private File dataFolder;

  /**
   * code folder location for this sketch (may not exist yet)
   */
  private File codeFolder;

  /**
   * Name of sketch, which is the name of main file (without .pde or .java
   * extension)
   */
  private String name;

  private List<SketchCode> codes = new ArrayList<SketchCode>();

  private static final Comparator<SketchCode> CODE_DOCS_COMPARATOR = new Comparator<SketchCode>() {
    @Override
    public int compare(SketchCode x, SketchCode y) {
      return x.getFileName().compareTo(y.getFileName());
    }
  };

  SketchData(File file) {
    primaryFile = file;

    // get the name of the sketch by chopping .pde or .java
    // off of the main file name
    String mainFilename = primaryFile.getName();
    int suffixLength = getDefaultExtension().length() + 1;
    name = mainFilename.substring(0, mainFilename.length() - suffixLength);

    folder = new File(file.getParent());
    //System.out.println("sketch dir is " + folder);
  }

  static public File checkSketchFile(File file) {
    // check to make sure that this .pde file is
    // in a folder of the same name
    String fileName = file.getName();
    File parent = file.getParentFile();
    String parentName = parent.getName();
    String pdeName = parentName + ".pde";
    File altPdeFile = new File(parent, pdeName);
    String inoName = parentName + ".ino";
    File altInoFile = new File(parent, inoName);

    if (pdeName.equals(fileName) || inoName.equals(fileName))
      return file;

    if (altPdeFile.exists())
      return altPdeFile;

    if (altInoFile.exists())
      return altInoFile;

    return null;
  }

  /**
   * Build the list of files.
   * <p>
   * Generally this is only done once, rather than
   * each time a change is made, because otherwise it gets to be
   * a nightmare to keep track of what files went where, because
   * not all the data will be saved to disk.
   * <p>
   * This also gets called when the main sketch file is renamed,
   * because the sketch has to be reloaded from a different folder.
   * <p>
   * Another exception is when an external editor is in use,
   * in which case the load happens each time "run" is hit.
   */
  protected void load() throws IOException {
    codeFolder = new File(folder, "code");
    dataFolder = new File(folder, "data");

    // get list of files in the sketch folder
    String list[] = folder.list();
    if (list == null) {
      throw new IOException("Unable to list files from " + folder);
    }

    // reset these because load() may be called after an
    // external editor event. (fix for 0099)
//    codeDocs = new SketchCodeDoc[list.length];
    clearCodeDocs();
//    data.setCodeDocs(codeDocs);

    for (String filename : list) {
      // Ignoring the dot prefix files is especially important to avoid files
      // with the ._ prefix on Mac OS X. (You'll see this with Mac files on
      // non-HFS drives, i.e. a thumb drive formatted FAT32.)
      if (filename.startsWith(".")) continue;

      // Don't let some wacko name a directory blah.pde or bling.java.
      if (new File(folder, filename).isDirectory()) continue;

      // figure out the name without any extension
      String base = filename;
      // now strip off the .pde and .java extensions
      for (String extension : EXTENSIONS) {
        if (base.toLowerCase().endsWith("." + extension)) {
          base = base.substring(0, base.length() - (extension.length() + 1));

          // Don't allow people to use files with invalid names, since on load,
          // it would be otherwise possible to sneak in nasty filenames. [0116]
          if (BaseNoGui.isSanitaryName(base)) {
            addCode(new SketchCode(new File(folder, filename)));
          } else {
            System.err.println(I18n.format(tr("File name {0} is invalid: ignored"), filename));
          }
        }
      }
    }

    if (getCodeCount() == 0)
      throw new IOException(tr("No valid code files found"));

    // move the main class to the first tab
    // start at 1, if it's at zero, don't bother
    for (SketchCode code : getCodes()) {
      //if (code[i].file.getName().equals(mainFilename)) {
      if (code.getFile().equals(primaryFile)) {
        moveCodeToFront(code);
        break;
      }
    }

    // sort the entries at the top
    sortCode();
  }

  public void save() throws IOException {
    for (SketchCode code : getCodes()) {
      if (code.isModified())
        code.save();
    }
  }

  public int getCodeCount() {
    return codes.size();
  }

  public SketchCode[] getCodes() {
    return codes.toArray(new SketchCode[0]);
  }

  /**
   * Returns the default extension for this editor setup.
   */
  public String getDefaultExtension() {
    return "ino";
  }

  /**
   * Returns a file object for the primary .pde of this sketch.
   */
  public File getPrimaryFile() {
    return primaryFile;
  }

  /**
   * Returns path to the main .pde file for this sketch.
   */
  public String getMainFilePath() {
    return primaryFile.getAbsolutePath();
    //return code[0].file.getAbsolutePath();
  }

  public void addCode(SketchCode sketchCode) {
    codes.add(sketchCode);
  }

  public void moveCodeToFront(SketchCode codeDoc) {
    codes.remove(codeDoc);
    codes.add(0, codeDoc);
  }

  protected void replaceCode(SketchCode newCode) {
    for (SketchCode code : codes) {
      if (code.getFileName().equals(newCode.getFileName())) {
        codes.set(codes.indexOf(code), newCode);
        return;
      }
    }
  }

  protected void sortCode() {
    if (codes.size() < 2)
      return;
    SketchCode first = codes.remove(0);
    Collections.sort(codes, CODE_DOCS_COMPARATOR);
    codes.add(0, first);
  }

  public SketchCode getCode(int i) {
    return codes.get(i);
  }

  protected void removeCode(SketchCode which) {
    for (SketchCode code : codes) {
      if (code == which) {
        codes.remove(code);
        return;
      }
    }
    System.err.println("removeCode: internal error.. could not find code");
  }

  public int indexOfCode(SketchCode who) {
    for (SketchCode code : codes) {
      if (code == who)
        return codes.indexOf(code);
    }
    return -1;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void clearCodeDocs() {
    codes.clear();
  }

  public File getFolder() {
    return folder;
  }

  public File getDataFolder() {
    return dataFolder;
  }

  public File getCodeFolder() {
    return codeFolder;
  }
}
