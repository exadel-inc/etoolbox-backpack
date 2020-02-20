(function(window) {
    $(window).adaptTo("foundation-registry").register("foundation.form.response.ui.error", {
        name: "errorResponseCreated",
        handler: function(form, data, xhr, error, errorThrown) {
            var title = Granite.I18n.get("Error");
            var message = xhr.responseJSON.log;

            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(title, message, "error");
        }
    });
})(window);