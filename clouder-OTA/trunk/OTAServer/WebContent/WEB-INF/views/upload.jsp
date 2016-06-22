<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Upload</title>
</head>
<body>
	<form action="upload" method="post" style="margin: 100px 0 0 200px" enctype="multipart/form-data">
		<div style="font-size: 20px">Upload</div></br>
		<div>
			<div style="width: 200px; float: left">Type: </div>
			<select name="type">
				<option value="apk">Apk</option>
				<option value="firmware">Firmware</option>
			</select>
		</div></br>
		<div>
			<div style="width: 200px; float: left">Choose File: </div>
			<input name="uploadFile" type="file"/>
		</div></br>
		<div>
			<div style="width: 200px; float: left">Version Name: </div>
			<input name="versionName" type="text"/>
		</div></br>
		<div>
			<div style="width: 200px; float: left">Version Code: </div>
			<input name="versionCode" type="text"/>
		</div></br>
		<button type="submit">Submit</button>
	</form>
</body>
</html>