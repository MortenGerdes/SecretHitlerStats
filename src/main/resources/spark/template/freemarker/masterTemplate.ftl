<#macro masterTemplate title="Welcome">
<!DOCTYPE html
        PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>SHStats</title>
</head>
<body>
<div class="page">
    <h1>Secret Hitler Stats</h1>
    <div class="body">
        <#nested />
    </div>
    <div class="footer">
        Secret Hitler Statistics &mdash; Made by Morten G
    </div>
</div>
</body>
</html>
</#macro>