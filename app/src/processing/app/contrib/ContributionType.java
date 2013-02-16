package processing.app.contrib;

public enum ContributionType {
//    LIBRARY, LIBRARY_COMPILATION, TOOL, MODE;
    LIBRARY, TOOL, MODE;

    
    public String toString() {
      switch (this) {
      case LIBRARY:
        return "library";
//      case LIBRARY_COMPILATION:
//        return "compilation";
      case TOOL:
        return "tool";
      case MODE:
        return "mode";
      }
      return null;  // should be unreachable
    };
    
    
    public String getFolderName() {
      switch (this) {
      case LIBRARY:
        return "libraries";
//      case LIBRARY_COMPILATION:
//        return "libraries";
      case TOOL:
        return "tools";
      case MODE:
        return "modes";
      }
      return null;  // should be unreachable
    }

    
    static public ContributionType fromName(String s) {
      if (s != null) {
        if ("library".equals(s.toLowerCase())) {
          return LIBRARY;
        }
//        if ("compilation".equals(s.toLowerCase())) {
//          return LIBRARY_COMPILATION;
//        }
        if ("tool".equals(s.toLowerCase())) {
          return TOOL;
        }
        if ("mode".equals(s.toLowerCase())) {
          return MODE;
        }
      }
      return null;
    }
    
    
//    static public boolean validName(String s) {
//      return "library".equals(s) || "tool".equals(s) || "mode".equals(s); 
//    }
  }