(function(window) {
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "backpack.download",
        handler: function(name, el, config, collection, selections) {
            window.location.href = config.data.href;
        }
    });
})(window);