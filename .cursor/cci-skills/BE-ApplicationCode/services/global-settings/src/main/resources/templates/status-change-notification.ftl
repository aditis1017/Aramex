<!DOCTYPE html>
<html>
<head>
    <style>
        body {
            font-family: Arial, sans-serif;
            font-size: 14px;
            color: #333;
        }
        .container {
            width: 100%;
            padding: 10px;
        }
        .header {
            margin-bottom: 15px;
        }
        .section-title {
            font-weight: bold;
            margin-top: 15px;
            margin-bottom: 5px;
        }
        .details-table {
            border-collapse: collapse;
            width: 100%;
        }
        .details-table td {
            padding: 6px;
            border: 1px solid #ddd;
        }
        .label {
            font-weight: bold;
            background-color: #f5f5f5;
            width: 30%;
        }
        .footer {
            margin-top: 20px;
        }
    </style>
</head>

<body>
<div class="container">

    <p>Dear ${userName},</p>

    <p>
        This is to inform you that the status of the following rule has been
        <b>updated</b> in the system.
    </p>

    <div class="section-title">Status Change Details:</div>

    <table class="details-table">
        <tr>
            <td class="label">Previous Status</td>
            <td>${oldStatus}</td>
        </tr>
        <tr>
            <td class="label">Current Status</td>
            <td>${newStatus}</td>
        </tr>
        <tr>
            <td class="label">Category</td>
            <td>${category}</td>
        </tr>
        <tr>
            <td class="label">Changed By</td>
            <td>${performedBy}</td>
        </tr>
        <tr>
            <td class="label">Date & Time</td>
            <td>${timestamp}</td>
        </tr>
    </table>

    <div class="section-title">Impact:</div>
    <p>
        This status change may impact system behavior, rule execution,
        and related workflows depending on the current configuration.
    </p>

    <#-- Optional Rule Link -->
    <#if ruleLink?? && ruleLink?has_content>
        <p>
            Please review the rule here:
            <a href="${ruleLink}">View Rule</a>
        </p>
    </#if>

    <p>
        If you have any questions or need further clarification, feel free to reach out.
    </p>

    <div class="footer">
        <p>Best regards,<br/>
        ${organizationName}</p>
    </div>

</div>
</body>
</html>