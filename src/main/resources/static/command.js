const xRequest = new XMLHttpRequest();

const updatePre = (text) => {
    const preCollection = document.getElementsByTagName('pre');
    for (i = 0; i < preCollection.length; ++i) {
        preCollection[i].innerText = text;
    }
};

xRequest.onreadystatechange = function () {
    if (this.readyState == 4 && this.status == 200) {
        const responseObject = JSON.parse(xRequest.responseText);
        let command = responseObject.command_info.command;
        for (const arg of (responseObject.command_info.args || [])) {
          command += ` ${arg}`;
        }
        let preText = `Now: ${new Date()}\n\n`;
        preText += `$ ${command}\n\n`;
        for (const outputLine of (responseObject.output_lines || [])) {
            preText += `${outputLine}\n`;
        }
        updatePre(preText);
    }
};

const requestData = (apiPath) => {
    xRequest.open('GET', apiPath, true);
    xRequest.setRequestHeader('Accept', 'application/json');
    xRequest.send();
};

const setTimer = (apiPath) => {
    const checkbox = document.getElementById('autoRefresh');

    setInterval(() => {
        if (checkbox.checked) {
            requestData(apiPath);
        }
    }, 1000);
};

const onload = (commandText, apiPath) => {
    let preText = `Now: ${new Date()}\n\n`;
    preText += `$ ${commandText}`;
    updatePre(preText);

    requestData(apiPath);

    setTimer(apiPath);
};
