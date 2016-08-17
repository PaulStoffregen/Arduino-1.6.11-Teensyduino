/*
  SketchCode - data class for a single file inside a sketch
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-08 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import processing.app.helpers.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static processing.app.I18n.tr;

/**
 * Represents a single tab of a sketch.
 */
public class SketchCode {

  /**
   * File object for where this code is located
   */
  private File file;

  /**
   * Text of the program text for this tab
   */
  private String program;

  private boolean modified;

  private Object metadata;

  public SketchCode(File file) {
    init(file, null);
  }

  public SketchCode(File file, Object metadata) {
    init(file, metadata);
  }

  private void init(File file, Object metadata) {
    this.file = file;
    this.metadata = metadata;

    try {
      load();
    } catch (IOException e) {
      System.err.println(
        I18n.format(tr("Error while loading code {0}"), file.getName()));
    }
  }


  public File getFile() {
    return file;
  }


  protected boolean fileExists() {
    return file.exists();
  }


  protected boolean fileReadOnly() {
    return !file.canWrite();
  }


  protected boolean deleteFile(Path tempBuildFolder) throws IOException {
    if (!file.delete()) {
      return false;
    }

    List<Path> tempBuildFolders = Stream.of(tempBuildFolder, tempBuildFolder.resolve("sketch"))
        .filter(path -> Files.exists(path)).collect(Collectors.toList());

    for (Path folder : tempBuildFolders) {
      if (!deleteCompiledFilesFrom(folder)) {
        return false;
      }
    }

    return true;
  }

  private boolean deleteCompiledFilesFrom(Path tempBuildFolder) throws IOException {
    List<Path> compiledFiles = Files.list(tempBuildFolder)
      .filter(pathname -> pathname.getFileName().toString().startsWith(getFileName()))
      .collect(Collectors.toList());

    for (Path compiledFile : compiledFiles) {
      try {
        Files.delete(compiledFile);
      } catch (IOException e) {
        return false;
      }
    }
    return true;
  }

  protected boolean renameTo(File what) {
    boolean success = file.renameTo(what);
    if (success) {
      file = what;
    }
    return success;
  }


  public String getFileName() {
    return file.getName();
  }


  public String getPrettyName() {
    String prettyName = getFileName();
    int dot = prettyName.lastIndexOf('.');
    return prettyName.substring(0, dot);
  }

  public String getFileNameWithExtensionIfNotIno() {
    if (getFileName().endsWith(".ino")) {
      return getPrettyName();
    }
    return getFileName();
  }

  public boolean isExtension(String... extensions) {
    return isExtension(Arrays.asList(extensions));
  }

  public boolean isExtension(List<String> extensions) {
    return FileUtils.hasExtension(file, extensions);
  }


  public String getProgram() {
    return program;
  }


  public void setProgram(String replacement) {
    program = replacement;
  }


  public int getLineCount() {
    return BaseNoGui.countLines(program);
  }


  public void setModified(boolean modified) {
    this.modified = modified;
  }


  public boolean isModified() {
    return modified;
  }


  /**
   * Load this piece of code from a file.
   */
  private void load() throws IOException {
    program = BaseNoGui.loadFile(file);

    if (program == null) {
      throw new IOException();
    }

    if (program.indexOf('\uFFFD') != -1) {
      System.err.println(
        I18n.format(
          tr("\"{0}\" contains unrecognized characters. " +
            "If this code was created with an older version of Arduino, " +
            "you may need to use Tools -> Fix Encoding & Reload to update " +
            "the sketch to use UTF-8 encoding. If not, you may need to " +
            "delete the bad characters to get rid of this warning."),
          file.getName()
        )
      );
      System.err.println();
    }

    setModified(false);
  }


  /**
   * Save this piece of code, regardless of whether the modified
   * flag is set or not.
   */
  public void save() throws IOException {
    // TODO re-enable history
    //history.record(s, SketchHistory.SAVE);

    BaseNoGui.saveFile(program, file);
    setModified(false);
  }


  /**
   * Save this file to another location, used by Sketch.saveAs()
   */
  public void saveAs(File newFile) throws IOException {
    BaseNoGui.saveFile(program, newFile);
  }


  public Object getMetadata() {
    return metadata;
  }


  public void setMetadata(Object metadata) {
    this.metadata = metadata;
  }
}
