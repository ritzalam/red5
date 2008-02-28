<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="/spring" %>
<%@ taglib prefix="spring-form" uri="/spring-form" %>
<head>
  <title>Red5 Admin</title>

  <meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type">
  <style type="text/css" media="screen">
html, body, #containerA, #containerB { height: 100%;
}
.formbg { background-color: rgb(238, 238, 238);
}
.formtable { border: 2px solid rgb(183, 186, 188);
}

.formtext { font-family: Arial,Helvetica,sans-serif;
    font-size: 12px;
    color: rgb(11, 51, 73);
}


body { margin: 0pt;
padding: 0pt;
overflow: hidden;
background-color: rgb(250, 250, 250);
}
.error { 
	font-family: Arial,Helvetica,sans-serif;
	font-size: 12px;
	color: red; 
}
  </style>
</head>
<body>
<table style="text-align: left; width: 100%; height: 100%;" border="0" cellpadding="0" cellspacing="10">

  <tbody>
    <tr>

      <td height="54"><img style="width: 136px; height: 54px;" alt="" src="assets/logo.png"></td>

    </tr>
    <tr class="formbg">

      <td align="center" valign="middle">
      
      <table style="width: 400px;" class="formtable" border="0" cellpadding="0" cellspacing="2">
      <tr>
      	<td class="formtext">&nbsp;<b>Register Admin User</b></td>
      </tr>
      <tr>
      <td>
      <form method="post" action="register.html">
        <table style="width: 400px;"  border="0" cellpadding="0" cellspacing="5">
          <tbody>

            <tr>
              <td align="right" width="20%" class="formtext">Username:</td>
		      <spring:bind path="userDetails.username">
		        <td width="20%">
		            <input name="username" value="<c:out value="${status.value}"/>">
		        </td>
		        <td width="60%" class="error">
		            <c:out value="${status.errorMessage}"/>
		        </td>
		      </spring:bind>
            </tr>
            <tr>
			  <td align="right" width="20%" class="formtext">Password:</td>
		      <spring:bind path="userDetails.password">
		        <td width="20%">
		          <input name="password" value="<c:out value="${status.value}"/>">
		        </td>
		        <td width="60%" class="error">
		          <c:out value="${status.errorMessage}"/>
		        </td>
		      </spring:bind>
            </tr>

            <tr>
              <td><input type="submit" value="Submit"></td>

              <td></td>
              <td class="error">
               <spring:hasBindErrors name="userDetails">
			    <b>Please fix all errors!</b>
			  </spring:hasBindErrors>  
              </td>

            </tr>
          </tbody>
        </table>
      </form>
      </td>
      </tr>
      </table>

      </td>
    </tr>

  </tbody>
</table>

<br>
</body>
</html>