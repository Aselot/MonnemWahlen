function showAlert() {
    alert("bruh")
}

function calcInputs(value) {
    var inputs, index;

    inputs = document.getElementsByTagName('input');
    counter = 0
    maxVotes = parseInt(inputs[0].value)
    flag = false
    for (index = 1; index < inputs.length; ++index) {
        elem = inputs[index]
        console.log(inputs[index].value)

        counter += parseInt(elem.value)
        if (counter > maxVotes) {
            flag = true
            elem.value = 0
        }
        // deal with inputs[index] element.
    }
    if (flag) {
        alert("you cant spend more than " + maxVotes + " votes")
    }
    console.log("index: ", index, ", counter: ", counter)

}

function pushedButton(worker_id, voter_id, poll_id, url) {

    inputs = document.getElementsByTagName('input');
    json = {
        "voter_id": voter_id,
        "worker_id": worker_id,
        "poll_id": poll_id,
        "vote": []
    }
    console.log(inputs)

    for (index = 1; index < inputs.length; ++index) {
        json["vote"][index] = inputs[index].value
    }

    var xhr = new XMLHttpRequest();
    xhr.open("POST", url+"/postBallot", true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    console.log(json, " ", voter_id, worker_id)


    xhr.onreadystatechange  = function () {
        if (xhr.readyState === XMLHttpRequest.DONE) {
                alert(xhr.responseText)
        }
    };

    xhr.send(JSON.stringify(json));
    console.log(xhr)
}
