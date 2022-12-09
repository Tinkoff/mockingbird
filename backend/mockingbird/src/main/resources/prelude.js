function randomInt(lbound, rbound) {
    if (typeof rbound === "undefined")
        return Math.floor(Math.random() * lbound);
    var min = Math.ceil(lbound);
    var max = Math.floor(rbound);
    return Math.floor(Math.random() * (max - min) + min);
}

function randomLong(lbound, rbound) {
    return randomInt(lbound, rbound);
}

function randomString(param1, param2, param3) {
    if (typeof param2 === "undefined" || typeof param3 === "undefined") {
        var result           = '';
        var characters       = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        var charactersLength = characters.length;
        for ( var i = 0; i < param1; i++ ) {
            result += characters.charAt(Math.floor(Math.random() * charactersLength));
        }
        return result;
    }

    var result           = '';
    var charactersLength = param1.length;
    for ( var i = 0; i < randomInt(param2, param3); i++ ) {
        result += param1.charAt(Math.floor(Math.random() * charactersLength));
    }
    return result;
}

function randomNumericString(length) {
    return randomString('0123456789', length, length + 1);
}

// https://stackoverflow.com/a/8809472/3819595
function UUID() { // Public Domain/MIT
    var d = new Date().getTime();//Timestamp
    var d2 = ((typeof performance !== 'undefined') && performance.now && (performance.now()*1000)) || 0;//Time in microseconds since page-load or 0 if unsupported
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16;//random number between 0 and 16
        if(d > 0){//Use timestamp until depleted
            r = (d + r)%16 | 0;
            d = Math.floor(d/16);
        } else {//Use microseconds since page-load if supported
            r = (d2 + r)%16 | 0;
            d2 = Math.floor(d2/16);
        }
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
}

function now(pattern) {
    var format = java.time.format.DateTimeFormatter.ofPattern(pattern);
    return java.time.LocalDateTime.now().format(format);
}

function today(pattern) {
    var format = java.time.format.DateTimeFormatter.ofPattern(pattern);
    return java.time.LocalDate.now().format(format);
}