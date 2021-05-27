/**
 * Login management
 */

(function() { // avoid variables ending up in the global scope

  document.getElementById("loginbutton").addEventListener('click', (e) => {
    e.preventDefault();
    var form = e.target.closest("form");
    if (form.checkValidity()) {
      makeCall("POST", 'CheckLogin', e.target.closest("form"),
        function(req) {
          if (req.readyState === XMLHttpRequest.DONE) {
            let message = req.responseText;
            e.target.closest("form").reset();
            switch (req.status) {
              case 200:
            	sessionStorage.setItem('username', message);
                window.location.href = "HomeACT.html";
                break;
              case 400: // bad request
                document.getElementById("errormessage").textContent = message;
                break;
              case 401: // unauthorized
                  document.getElementById("errormessage").textContent = message;
                  break;
              case 500: // server error
            	document.getElementById("errormessage").textContent = message;
                break;
            }
          }
        }
      , false);
    } else {
    	 form.reportValidity();
    }
  });

})();