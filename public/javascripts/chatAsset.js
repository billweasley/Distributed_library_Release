  var $messages = $("#messages"),
                        $send = $("#send"),
                        $message = $("#message"),
                        connection = new WebSocket("@url");

                $send.prop("disabled", true);

                var send = function () {
                    var text = $message.val();
                    $message.val("");
                  var obj = new Object();
                    obj.from = 1;
                    obj.to = 2;
                    obj.msg = text;
                    connection.send(obj);
                };

                connection.onopen = function () {
                    $send.prop("disabled", false);
                    $messages.prepend($("<li class='bg-info' style='font-size: 1.5em'>Connected</li>"));
                    $send.on('click', send);
                    $message.keypress(function(event){
                        var keycode = (event.keyCode ? event.keyCode : event.which);
                        if(keycode == '13'){
                            send();
                        }
                    });
                };
                connection.onerror = function (error) {
                    console.log('WebSocket Error ', error);
                };
                connection.onmessage = function (event) {
                    $messages.append($("<li style='font-size: 1.5em'>" + event.msg + "</li>"))
                }
