const stringify = JSON.stringify;
const stringifyPretty = (object) => stringify(object, null, 2);

const xRequest = new XMLHttpRequest();

const updatePre = (text) => {
    const preCollection = document.getElementsByTagName('pre');
    for (i = 0; i < preCollection.length; ++i) {
        preCollection[i].innerText = text;
    }
};

xRequest.onreadystatechange = function () {
    if (this.readyState == 4 && this.status == 200) {
        const responseObject = JSON.parse(this.responseText);
        let preText = `Now: ${new Date()}\n\n`;
        preText += stringifyPretty(responseObject);
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

const onload = (apiPath) => {
    let preText = `Now: ${new Date()}\n\n`;
    updatePre(preText);

    requestData(apiPath);

    setTimer(apiPath);
};
