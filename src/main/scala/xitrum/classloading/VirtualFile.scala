package xitrum.classloading

import java.io.File

class VirtualFile(file : File) {
  
  val realFile: File = file
  
  // Some basic functions of a file
  def getName() : String = realFile.getName()
  def isDirectory() : Boolean = realFile.isDirectory()
  def exists() : Boolean = try { realFile.exists() } catch { case e:Exception => false }
  def lastModified() : Long = realFile.lastModified()
  def length() : Long = realFile.length()
  def getRealFile() : File = realFile
  
  def list() : List[VirtualFile] = {
    var res = List[VirtualFile]()
    if (exists()) {
      var item = null
      val children : Array[File] = realFile.listFiles()
      for (item <- children) {
        res = res.::(new VirtualFile(item))
      }
    }
    res
  }
  
}