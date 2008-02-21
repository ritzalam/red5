<%@ page session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<html>
<head>
  <title><fmt:message key="title"/></title>
  <style>
    .error { color: red; }
  </style>  
</head>
<body>
<h1>Register Admin User</h1>
<form:form method="post" commandName="commandBean">
  <table width="95%" bgcolor="f8f8ff" border="0" cellspacing="0" cellpadding="5">
    <tr>
      <td align="right" width="20%">Username:</td>
        <td width="20%">
          <form:input path="username"/>
        </td>
        <td width="60%">
          <form:errors path="username" cssClass="error"/>
        </td>
    </tr>
    <tr>
      <td align="right" width="20%">Password:</td>
        <td width="20%">
          <form:input path="password"/>
        </td>
        <td width="60%">
          <form:errors path="password" cssClass="error"/>
        </td>
    </tr>
  </table>
  <br>
  <input type="submit" align="center" value="Submit">
</form:form>
</body>
</html>