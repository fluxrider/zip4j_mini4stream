/*
 * Copyright 2019 David Lareau  
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

/*
Much like the original zip4j library, ZipOutputStream can be used to 
create zip files "on the fly" with all compression and encryption being
done in memory.

lib4j_mini4stream adds a functionality akin to ZipFile.addStream() to ZipOutputStream.

lib4j_mini4stream is stripped down to:
- ZipOutputStream only (no ZipFile).
- only a single file in the zip, no directories.
- no unzipping.
*/

import java.io.*;
import net.lingala.zip4j.io.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class Example {

  public static void main(String[] args) throws Exception {
    ZipParameters params = new ZipParameters();
    params.setEncryptFiles(true);
    params.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
    params.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
    params.setPassword("my_password_goes_here");
    params.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
    params.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
    // TODO try MAXIMUM/ULTRA

    ZipOutputStream out = new ZipOutputStream(new FileOutputStream("example.zip"));
    out.putNextEntry(new File("LICENSE"), params); // TMP the point is that I won't need a file no more
    out.write("Hello".getBytes());
    out.closeEntry();
    out.finish();
  }

}