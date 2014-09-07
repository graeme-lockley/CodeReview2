$(document).ready(function () {
    $(function () {
        var FeedbackView = Backbone.View.extend({
            initialize: function () {
                this.listenTo(this.model, 'sync', this.render);
                this.model.fetch();
            },
            render: function () {
                var prefix = this.$el.attr("id");
                this.$el.html(_.template(templateOnName("portlets/feedback/main.html"), {feedback: this.model, prefix: prefix}));

                this.model.forEach(function (feedbackItem) {
                    var x = new FeedbackItemView({model: feedbackItem, el: "#" + prefix + "-" + feedbackItem.id});
                });

                return this;
            }
        });

        $(".feedback").each(function (idx, dom) {
            var feedback = new Feedback();
            feedback.url = $(dom).attr("data-collection");
            var feedbackView = new FeedbackView({model: feedback, el: "#" + $(dom).attr("id")});
        });
    });
});