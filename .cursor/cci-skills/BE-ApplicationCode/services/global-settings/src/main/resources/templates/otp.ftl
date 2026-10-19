<!DOCTYPE html>
<html>
<head>
    <style>
        table.gridtable {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
        }

        table.gridtable th,
        table.gridtable td {
            border: 1px solid #000;
            padding: 8px;
        }

        table.gridtable th {
            background-color: #d3d3d3;
            text-align: left;
        }
    </style>
</head>
<body>

<table class="gridtable">

    <!-- Participant Info -->
    <tr>
        <th colspan="3">Participant's Name</th>
        <th colspan="2">Client ID</th>
    </tr>
    <tr>
        <td colspan="3">${clientName!"-"}</td>
        <td colspan="2">${clientId!"-"}</td>
    </tr>

    <!-- Horse Table Header -->
    <tr>
        <th>Horse Name</th>
        <th>Dam</th>
        <th>Sire</th>
        <th>Foal Date</th>
        <th>Certificate No</th>
    </tr>

    <!-- Horse Data -->
    <#if series.futurityNominationDTOList?has_content>
        <#list series.futurityNominationDTOList as horse>
            <tr>
                <td>${horse.horseName!"-"}</td>
                <td>${horse.dam!"-"}</td>
                <td>${horse.sire!"-"}</td>
                <td>
                    <#if horse.foalDate??>
                        ${horse.foalDate?datetime?string('dd-MM-yyyy')}
                    <#else>
                        -
                    </#if>
                </td>
                <td>${horse.certificateNo!"-"}</td>
            </tr>
        </#list>
    <#else>
        <tr>
            <td colspan="5">No data available</td>
        </tr>
    </#if>

</table>

</body>
</html>