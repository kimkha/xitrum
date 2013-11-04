package xitrum.classloading

import java.io.File

class ApplicationClassloader extends ClassLoader {
  
  def scanAllClasses(): ApplicationClass = {
    println(">>>>>>>>> getAllClasses ");
    
    val root = new VirtualFile(new File("src/main/scala"))
    getAllClasses(root)
    
    null
  }

  def detectChanges() {
    println(">>>>>>>>> Detect changes " + this.getPackages().length);
  }
  
  private def getAllClasses(rootVf : VirtualFile) : List[ApplicationClass] = {
    val all = List[ApplicationClass]()
    var vf = null

    for (vf <- rootVf.list) {
      scan(all, "", vf)
    }
    
    all
  }
  
  private def scan(classes : List[ApplicationClass], packageName : String, currentVf : VirtualFile) {
    if (currentVf.isDirectory) {
      var vf = null
      for (vf <- currentVf.list) {
        scan(classes, packageName + currentVf.getName + ".", vf)
      }
    } else {
      if ((currentVf.getName.endsWith(".scala") || currentVf.getName.endsWith(".java")) && !currentVf.getName.startsWith(".")) {
        val className = packageName + currentVf.getName.split("\\.")(0)
        println(className)
        val clazz = this.findClass(className)
        println("> " + clazz)
      }
    }
  }
  
}

object Test {
  def main(args: Array[String]) {
    var cl = new ApplicationClassloader()
    cl.scanAllClasses()
  }
}