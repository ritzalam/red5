package org.electroteque.security;

import java.util.List;
import org.red5.server.api.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectSecurity;

public class SharedObjectSecurity implements ISharedObjectSecurity {
  
  public boolean isConnectionAllowed(ISharedObject so) {
      // Note: we don't check for the name here as only one SO can be
      //       created with this handler.
      return false;
  }
  
  public boolean isCreationAllowed(IScope scope, String name,
    boolean persistent) {
      return false;
  }
  
  public boolean isDeleteAllowed(ISharedObject so, String key) {
      return false;
  }
  
  public boolean isSendAllowed(ISharedObject so, String message,
    List arguments) {
      return false;
  }
  
  public boolean isWriteAllowed(ISharedObject so, String key,
    Object value) {
      return false;
  }
  
}