
(function (document, $) {
    "use strict";
    var flag = false;
    $(document).on("foundation-contentloaded", function (e) {
        switchShowHideHandler($(".cq-dialog-switch-showhide", e.target));
    });

    $(document).on("change", ".cq-dialog-switch-showhide", function (e) {
        switchShowHideHandler($(this));
    });

    function switchShowHideHandler(el) {
        el.each(function (i, element) {
            var paths = $(".cq-dialog-switch-paths");
            var query = $(".cq-dialog-switch-query");
            if (flag) {
                flag = false;
                paths.parent().hide();
                paths.attr("aria-required", "false");
                $(".cq-dialog-switch-paths :input").attr("disabled", "disabled");
                query.parent().show();
                query.removeAttr("disabled");
            } else {
                flag = true;
                paths.parent().show();
                $(".cq-dialog-switch-paths :input").removeAttr("disabled");
                paths.attr("aria-required", "true");
                query.parent().hide();
                query.attr("disabled", "disabled");
                }
        })
    }

})(document, Granite.$);